/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
 * 
 * This file is part of the KNIME CDK plugin.
 * 
 * The KNIME CDK plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The KNIME CDK plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with the plugin. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.knime.nodes.sugarremover;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.Atom;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IRing;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.BondManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Multi threaded worker implementation for the Sugar Remover Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SugarRemoverWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final List<IAtomContainer> sugarChains;
	private final BufferedDataContainer bdc;
	private final SugarRemoverSettings settings;

	private boolean explicitH_flag;
	private final UniversalIsomorphismTester isomorphismTester = new UniversalIsomorphismTester();

	public SugarRemoverWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, final SugarRemoverSettings settings,
			final List<IAtomContainer> sugarChains) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.settings = settings;
		this.columnIndex = columnIndex;
		this.sugarChains = sugarChains;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else {
			CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);

			IAtomContainer oldMol = cdkCell.getAtomContainer();
			IAtomContainer clonedMol;

			try {
				clonedMol = (IAtomContainer) oldMol.clone();

				// keep track of the state of the Hydrogens
				SMSDNormalizer.convertExplicitToImplicitHydrogens(clonedMol);
				explicitH_flag = (oldMol.getAtomCount() != clonedMol.getAtomCount());
				// AtomContainerManipulator.convertImplicitToExplicitHydrogens(((CDKCell) newMols.get(i))
				// .getAtomContainer()); if (!explicitH_flag) newMols.set(i, (CDKCell)
				// CDKCell.createCDKCell(SMSDNormalizer .convertExplicitToImplicitHydrogens(((CDKCell)
				// newMols.get(i)).getAtomContainer()))); }
				outCell = removeSugars(clonedMol);
			} catch (CloneNotSupportedException e) {
				outCell = DataType.getMissingCell();
			}
		}

		if (settings.replaceColumn()) {
			row = new ReplacedColumnsDataRow(row, outCell, columnIndex);
		} else {
			row = new AppendedColumnRow(row, outCell);
		}
		return row;
	}

	/**
	 * remove the sugar rings from a molecule
	 */
	private DataCell removeSugars(IAtomContainer atomContainer) {

		// find all the rings in the molecule
		SSSRFinder molecule_ring = new SSSRFinder(atomContainer);
		IRingSet ringset = molecule_ring.findSSSR();
		boolean[] rings2Del = new boolean[ringset.getAtomContainerCount()];
		// for each ring tag the ones to be deleted
		for (int i = 0; i < ringset.getAtomContainerCount(); i++) {
			IAtomContainer one_ring = ringset.getAtomContainer(i);
			// get the molecular formula of the ring
			IMolecularFormula molecularFormula = MolecularFormulaManipulator.getMolecularFormula(one_ring);
			String formula = MolecularFormulaManipulator.getString(molecularFormula);
			// get the the bond order of all the atoms in the ring (atom
			// container)
			// bonds outside of the ring are not considered
			IBond.Order bondorder = AtomContainerManipulator.getMaximumBondOrder(one_ring);
			// if all bonds between the atoms in the ring are single and the
			// rings have formula below
			if (IBond.Order.SINGLE.equals(bondorder)
					& (formula.equals("C5O") | formula.equals("C4O") | formula.equals("C6O"))) {
				// check if there is a glycoside bond
				rings2Del[i] = hasGlycosideBond(one_ring, atomContainer, ringset);

			}
		}
		// remove all the rings with a glycosidic bond
		// the remaining atoms in the atom container will be cleaned bellow
		for (int i = 0; i < rings2Del.length; i++) {
			if (rings2Del[i]) {
				for (IAtom atom : ringset.getAtomContainer(i).atoms()) {
					atomContainer.removeAtomAndConnectedElectronContainers(atom);
				}
			}
		}
		Map<Object, Object> properties = atomContainer.getProperties();
		// get the set of atom containers remaining after removing the glycoside bond
		IAtomContainerSet molset = ConnectivityChecker.partitionIntoMolecules(atomContainer);
		IAtomContainer finalAtomContainer = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
		// Remove all sugars with an open ring structure
		if (molset.getAtomContainerCount() == 1) {
			if (explicitH_flag)
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(molset.getAtomContainer(0));
			finalAtomContainer.add(molset.getAtomContainer(0));
		} else {
			for (int i = 0; i < molset.getAtomContainerCount(); i++) {

				// set the properties of the atom container as in the original molecule
				molset.getAtomContainer(i).setProperties(properties);
				int size = molset.getAtomContainer(i).getBondCount();
				// remove atomcontainers with less than 5 bonds, this may be too restrictive
				if (size >= 5) {
					if (!hasSugarChains(molset.getAtomContainer(i), ringset.getAtomContainerCount())) {
						if (explicitH_flag)
							AtomContainerManipulator.convertImplicitToExplicitHydrogens(molset.getAtomContainer(i));
						try {
							IAtomContainer mol = molset.getAtomContainer(i);
							CDKNodeUtils.getStandardMolecule(mol);
							finalAtomContainer.add(mol);
						} catch (Exception exception) {
							finalAtomContainer = null;
							break;
						}
					}
				}
			}
		}

		return (finalAtomContainer == null || finalAtomContainer.getAtomCount() == 0) ? DataType.getMissingCell()
				: CDKCell.createCDKCell(finalAtomContainer);
	}

	/**
	 * check is the remaining molecule is a sugar
	 * 
	 * @param molecule
	 * @param atomContainerCount
	 * @return
	 */
	private boolean hasSugarChains(final IAtomContainer molecule, final int atomContainerCount) {

		boolean isSubstructure = false;
		int ringCount = atomContainerCount;
		if (ringCount == 0) {
			try {
				for (IAtomContainer atomcontainer : sugarChains) {
					IAtomContainer query = atomcontainer;
					isSubstructure = isomorphismTester.isSubgraph(molecule, query);
					return isSubstructure;
				}
			} catch (CDKException ex) {
				// nothing to do
			}

		}
		return false;
	}

	/**
	 * this method checks if there is a glycoside bond or not
	 * 
	 * @param ring
	 * @param molecule
	 * @param ringset
	 * @return
	 */
	private boolean hasGlycosideBond(final IAtomContainer ring, final IAtomContainer molecule, final IRingSet ringset) {

		IAtomContainer sugarRing = ring;
		IRingSet sugarRingsSet = ringset;
		IRingSet connectedRings = sugarRingsSet.getConnectedRings((IRing) ring);

		List<IAtom> connectedAtoms = new ArrayList<IAtom>();
		List<IBond> bonds = new ArrayList<IBond>();

		// get all bonds including of atoms attached to the ring
		for (IAtom atom : sugarRing.atoms()) {
			bonds = molecule.getConnectedBondsList(atom);
		}
		// if there are no other sugar rings connected and the atoms in the ring
		// do not have any double bond

		if (IBond.Order.SINGLE.equals(BondManipulator.getMaximumBondOrder(bonds))
				&& connectedRings.getAtomContainerCount() == 0) {
			// get connected atoms of atoms in sugar ring to check if there is a
			// glycoside bond
			// gycoside bond: a bond is established between one of the oxygens
			// connected to the ring and a carbon from a substituent group
			for (IAtom atom : sugarRing.atoms()) {
				List<IAtom> conn_Atoms = molecule.getConnectedAtomsList(atom);
				connectedAtoms.addAll(conn_Atoms);
			}
			// go through all the atoms of the ring
			for (IAtom connected_atom : connectedAtoms) {
				// if the atom is not part of the ring and is an oxygen
				if (!sugarRing.contains(connected_atom)
						&& connected_atom.getSymbol().matches((new Atom("O").getSymbol()))) {
					// check if the oxygen is between two carbons
					// one of the carbons has to be part of the ring
					boolean c_ring = false;
					boolean c_other = false;
					List<IAtom> conn_Atoms = molecule.getConnectedAtomsList(connected_atom);
					for (IAtom neighbour : conn_Atoms) {
						// check if the carbon is in from the ring
						c_ring = c_ring || sugarRing.contains(neighbour);
						// check if the carbon is not from the ring and is a
						// carbon from a substituent group
						c_other = c_other
								|| (!sugarRing.contains(neighbour) && !neighbour.getSymbol().matches(
										new Atom("H").getSymbol()));
					}
					// stop iteration if there is a glycoside bond
					if (c_ring && c_other)
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * this method returns all the a glycoside bond in the iatomcontainer attaching the oxygen to the carbon in the
	 * sugar ring
	 * 
	 * @param ring
	 * @param molecule
	 * @param ringset
	 * @return
	 */
	@SuppressWarnings("unused")
	private IBond getGlycosideBond(final IAtomContainer ring, final IAtomContainer molecule, final IRingSet ringset) {

		IAtomContainer sugarRing = ring;
		IRingSet sugarRingsSet = ringset;
		IRingSet connectedRings = sugarRingsSet.getConnectedRings((IRing) ring);

		List<IAtom> connectedAtoms = new ArrayList<IAtom>();
		List<IBond> bonds = new ArrayList<IBond>();

		// get all bonds including of atoms attached to the ring
		for (IAtom atom : sugarRing.atoms()) {
			bonds = molecule.getConnectedBondsList(atom);
		}
		// if there are no other sugar rings connected and the atoms in the ring
		// do not have any double bond
		if (IBond.Order.SINGLE.equals(BondManipulator.getMaximumBondOrder(bonds))
				&& connectedRings.getAtomContainerCount() == 0) {

			// get connected atoms of atoms in sugar ring to check if there is a
			// glycoside bond
			// gycoside bond: a bond is established between one of the oxygens
			// connected to the ring and a carbon from a substituent group
			for (IAtom atom : sugarRing.atoms()) {
				List<IAtom> conn_Atoms = molecule.getConnectedAtomsList(atom);
				connectedAtoms.addAll(conn_Atoms);
			}
			// go through all the atoms of the ring
			for (IAtom connected_atom : connectedAtoms) {
				// if the atom is not part of the ring and is an oxygen
				if (!sugarRing.contains(connected_atom)
						&& connected_atom.getSymbol().matches((new Atom("O").getSymbol()))) {
					// check if the oxygen is between two carbons
					// one of the carbons has to be part of the ring
					List<IAtom> conn_Atoms = molecule.getConnectedAtomsList(connected_atom);
					for (IAtom neighbour : conn_Atoms) {
						// check if the carbon is not from the ring and is a
						// carbon from a substituent group
						if (!sugarRing.contains(neighbour) && neighbour.getSymbol().matches(new Atom("C").getSymbol()))
							// get the molecule bond
							return molecule.getBond(neighbour, connected_atom);
					}

				}
			}
		}
		return null;
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		DataRow append = task.get();
		if (!append.getCell(columnIndex).isMissing()) {
			bdc.addRowToTable(append);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
