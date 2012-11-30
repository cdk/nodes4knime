/*
 * Copyright (c) 2012, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.sugarremover;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.RowAppender;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.Atom;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IRing;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.ringsearch.SSSRFinder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.BondManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of SugarRemover.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SugarRemoverNodeModel extends ThreadedTableBuilderNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SugarRemoverNodeModel.class);

	private final SugarRemoverSettings m_settings = new SugarRemoverSettings();

	private int molColIndex;
	private List<IAtomContainer> sugarChains;
	private final UniversalIsomorphismTester isomorphismTester = new UniversalIsomorphismTester();

	private boolean explicitH_flag = false;

	/**
	 * Constructor for the node model.
	 */
	protected SugarRemoverNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		String[] smilesList = { "C(C(C(C(C(C=O)O)O)O)O)O", "C(C(CC(C(CO)O)O)O)(O)=O", "C(C(C(CC(=O)O)O)O)O",
				"C(C(C(C(C(CO)O)O)O)=O)O", "C(C(C(C(C(CO)O)O)O)O)O", "C(C(C(C(CC=O)O)O)O)O", "occ(o)co",
				"OCC(O)C(O)C(O)C(O)CO", "O=CC(O)C(O)C(O)C(O)CO", "CC(=O)OCC(O)CO", "CCCCC(O)C(=O)O",
				"CC(=O)CC(=O)CCC(=O)O", "CC(O)C(O)C(=O)O", "O=C(O)CC(O)CC(=O)O", "O=C(O)C(=O)C(=O)C(O)C(O)CO",
				"CC(O)CC(=O)O", "CC(CCC(=O)O)CC(=O)O", "O=C(O)CCC(O)C(=O)O", "O=CC(O)C(O)C(O)C(O)CO",
				"O=C(CO)C(O)C(O)CO" };
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		sugarChains = new ArrayList<IAtomContainer>();

		try {
			for (String smiles : smilesList) {
				sugarChains.add(sp.parseSmiles(smiles));
			}
		} catch (InvalidSmilesException ex) {
			LOGGER.error(ex.getMessage());
		}

		molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());

		DataColumnSpec[] colSpecs = createColSpec(data[0].getDataTableSpec());
		return new DataTableSpec[] { new DataTableSpec(colSpecs) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		if (inRow.getCell(molColIndex).isMissing()) {
			if (!m_settings.replaceColumn()) {
				outputTables[0].addRowToTable(new AppendedColumnRow(inRow, DataType.getMissingCell()));
			} else {
				outputTables[0].addRowToTable(inRow);
			}
			return;
		} else {
			IAtomContainer oldMol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();
			IAtomContainer clonedMol = (IAtomContainer) oldMol.clone();

			// keep track of the state of the Hydrogens
			SMSDNormalizer.convertExplicitToImplicitHydrogens(clonedMol);
			explicitH_flag = (oldMol.getAtomCount() != clonedMol.getAtomCount());

			List<DataCell> newMols = removeSugars(clonedMol);
			if (newMols.isEmpty()) {
				if (!m_settings.replaceColumn()) {
					outputTables[0].addRowToTable(new AppendedColumnRow(inRow, DataType.getMissingCell()));
				} else {
					outputTables[0].addRowToTable(inRow);
				}
				return;
			} else {
				for (int i = 0; i < newMols.size(); i++) {

					AtomContainerManipulator.convertImplicitToExplicitHydrogens(((CDKCell) newMols.get(i))
							.getAtomContainer());
					if (!explicitH_flag)
						newMols.set(
								i,
								new CDKCell(
										SMSDNormalizer.convertExplicitToImplicitHydrogens(((CDKCell) newMols.get(i))
												.getAtomContainer())));
					if (!m_settings.replaceColumn()) {
						outputTables[0].addRowToTable(new AppendedColumnRow(new RowKey(inRow.getKey().getString() + "_"
								+ i), inRow, newMols.get(i)));
					} else {
						DataCell[] replacedCells = new DataCell[inRow.getNumCells()];
						int k = 0;
						for (DataCell cell : inRow) {
							if (k == molColIndex) {
								replacedCells[k] = newMols.get(i);
							} else {
								replacedCells[k] = cell;
							}
							k++;
						}
						outputTables[0].addRowToTable(new DefaultRow(new RowKey(inRow.getKey().getString() + "_" + i),
								replacedCells));
					}
				}
			}
		}
	}

	/**
	 * remove the sugar rings from a molecule
	 */
	private List<DataCell> removeSugars(IAtomContainer atomContainer) {

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
		// get the set of atom containers remaining after removing the glycoside
		// bond
		IAtomContainerSet molset = ConnectivityChecker.partitionIntoMolecules(atomContainer);
		List<DataCell> remainingMols = new ArrayList<DataCell>(molset.getAtomContainerCount());
		int addedMol = 0;
		// Remove all sugars with an open ring structure
		for (int i = 0; i < molset.getAtomContainerCount(); i++) {

			// set the properties of the atom container as in the original
			// molecule
			molset.getAtomContainer(i).setProperties(properties);
			int size = molset.getAtomContainer(i).getBondCount();
			// remove atomcontainers with less than 5 bounds
			// this may be too restrictive
			if (size >= 5) {
				if (!hasSugarChains(molset.getAtomContainer(i), ringset.getAtomContainerCount())) {
					if (explicitH_flag)
						AtomContainerManipulator.convertImplicitToExplicitHydrogens(molset.getAtomContainer(i));
					try {
						IAtomContainer mol = molset.getAtomContainer(i);
						CDKNodeUtils.getStandardMolecule(mol);
						remainingMols.add(new CDKCell(mol));
					} catch (Exception exception) {
						remainingMols.add(DataType.getMissingCell());
					}
					addedMol++;
				}
			}
		}
		return remainingMols.subList(0, addedMol);
	}

	/**
	 * check is the remaining molecule is a sugar
	 * 
	 * @param molecule
	 * @param atomContainerCount
	 * @return
	 */
	private boolean hasSugarChains(IAtomContainer molecule, int atomContainerCount) {

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
				// TODO say something

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
	private boolean hasGlycosideBond(IAtomContainer ring, IAtomContainer molecule, IRingSet ringset) {

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
	private IBond getGlycosideBond(IAtomContainer ring, IAtomContainer molecule, IRingSet ringset) {

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// set the molcolumn, check if is compatible, etc...
		int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (molCol >= 0) {
						molCol = -1;
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
					}
				}
			}

			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				m_settings.molColumnName(name);
			}
		}

		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColumnName() + "' does not exist");
		}

		// check if there is any issue with the name of the column to be appended
		if (!m_settings.replaceColumn()) {
			String name = m_settings.appendColumnName();

			if (name == null || name.length() == 0) {
				throw new InvalidSettingsException("Invalid name for appended column");
			}
			if (inSpecs[0].containsName(name)) {
				throw new InvalidSettingsException("Duplicate column name: " + name);
			}
		}

		DataColumnSpec[] colSpecs = createColSpec(inSpecs[0]);

		return new DataTableSpec[] { new DataTableSpec(colSpecs) };
	}

	/**
	 * this method creates all the Column Specifications
	 * 
	 * @param in
	 * @return
	 */
	private DataColumnSpec[] createColSpec(final DataTableSpec in) {

		DataColumnSpec[] colSpecs = new DataColumnSpec[in.getNumColumns() + (m_settings.replaceColumn() ? 0 : 1)];
		// iterate through the column specs and copy them to the new column
		// specs
		for (int i = 0; i < in.getNumColumns(); i++) {
			colSpecs[i] = in.getColumnSpec(i);
		}
		// if it is not to replace the column
		if (!m_settings.replaceColumn()) {
			colSpecs[colSpecs.length - 1] = new DataColumnSpecCreator(m_settings.appendColumnName(), CDKCell.TYPE)
					.createSpec();
		}
		return colSpecs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettings(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SugarRemoverSettings s = new SugarRemoverSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// do nothing
	}
}
