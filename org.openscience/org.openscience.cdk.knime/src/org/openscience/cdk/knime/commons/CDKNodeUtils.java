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
package org.openscience.cdk.knime.commons;

import java.awt.Color;
import java.io.StringReader;

import org.knime.chem.types.CMLValue;
import org.knime.chem.types.InchiValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.hash.BasicAtomEncoder;
import org.openscience.cdk.hash.HashGeneratorMaker;
import org.openscience.cdk.hash.MoleculeHashGenerator;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.FixBondOrdersTool;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Utility functions for CDK object standardisation.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKNodeUtils {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKNodeUtils.class);
	private static final CDKHydrogenAdder HADDER = CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance());
	private static final Aromaticity AROMATICITY = new Aromaticity(ElectronDonation.daylight(), Cycles.allOrVertexShort());
	private static final SmilesGenerator SG = SmilesGenerator.isomeric().aromatic();
	private static final SmilesParser SR = new SmilesParser(SilentChemObjectBuilder.getInstance());
	private static final FixBondOrdersTool BONDFIXTOOL = new FixBondOrdersTool();

	private static final MoleculeHashGenerator GENERATOR = new HashGeneratorMaker().depth(8).charged().molecular();
	private static final MoleculeHashGenerator GENERATOR_FULL = new HashGeneratorMaker().depth(8).charged()
			.encode(BasicAtomEncoder.BOND_ORDER_SUM).chiral().isotopic().radical().molecular();

	private static InChIGeneratorFactory ig;
	static {
		try {
			ig = InChIGeneratorFactory.getInstance();
			ig.setIgnoreAromaticBonds(true);
		} catch (CDKException e) {
			LOGGER.error("Failed to load the InChIGeneratorFactory.");
		}
	}

	/** Array with the value classes that all CDK nodes accept by default. */
	@SuppressWarnings("unchecked")
	public static final Class<? extends DataValue>[] ACCEPTED_VALUE_CLASSES = new Class[] { CDKValue.class,
			SdfValue.class, SmilesValue.class, CMLValue.class, InchiValue.class };

	/**
	 * Gets the standardised CDK KNIME molecule with implicit hydrogens and
	 * detected aromaticity.
	 * 
	 * @param molecule the untyped CDK molecule
	 * @param calcCoordinates whether to calculate 2D coordinates
	 * @throws CDKException description of the exception
	 */
	public static IAtomContainer getFullMolecule(final IAtomContainer molecule) throws CDKException {

		try {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
			for (IAtom atom : molecule.atoms()) {
				if (atom instanceof IPseudoAtom) {
					atom.setAtomicNumber(0);
					if (atom.getImplicitHydrogenCount() == null || atom.getImplicitHydrogenCount() < 0) {
						atom.setImplicitHydrogenCount(0);
					}
				} else if (atom.getImplicitHydrogenCount() == null || atom.getImplicitHydrogenCount() < 0) {
					HADDER.addImplicitHydrogens(molecule, atom);
				}
			}
			AROMATICITY.apply(molecule);
		} catch (IllegalAccessError error) {
			throw new CDKException("Illegal Access Error - QueryChemObject." + error);
		} catch (Exception exception) {
			throw new CDKException("Exception during conversion.", exception);
		}
		
		return molecule;
	}

	public static IAtomContainer getFullMolecule(String smiles) {
		try {
			IAtomContainer mol = SR.parseSmiles(smiles);
			mol = getFullMolecule(mol);
			return mol;
		} catch (Exception exception) {
			LOGGER.error("SMILES conversion error:", exception);
			return null;
		}
	}
	
	public static IAtomContainer fixBondOrder(IAtomContainer mol) {
		
		try {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
			mol = BONDFIXTOOL.kekuliseAromaticRings(mol);
		} catch (Exception exception) {
			LOGGER.error("Bond error correction error:", exception);
			return null;
		}
		
		return mol;
	}

	/**
	 * Gets the clone of the CDK KNIME input molecule with all hydrogens set as
	 * explicit hydrogens.
	 * 
	 * @param molecule the input CDK molecule
	 * @return the CDK molecule clone with explicit hydrogens
	 * @throws CDKException description of the exception
	 */
	public synchronized static IAtomContainer getExplicitClone(final IAtomContainer molecule) throws CDKException {

		IAtomContainer clone;

		try {
			clone = (IAtomContainer) molecule.clone();
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(clone);
			AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(clone);
		} catch (CloneNotSupportedException exception) {
			throw new CDKException(exception.getMessage());
		}

		return clone;
	}

	/**
	 * Calculates 2D coordinates for the CDK molecule. If 'forced', the
	 * coordinates will be generated even if the molecule has 2D coordinates
	 * already.
	 * 
	 * @param molecule the CDK molecule
	 * @param force whether to force the calculation of 2D coordinates
	 * @param clone whether to clone the CDK molecule
	 * @throws CDKException description of the exception
	 */
	public static IAtomContainer calculateCoordinates(IAtomContainer molecule, final boolean force, final boolean clone)
			throws CDKException {

		if (force || (!(GeometryTools.has2DCoordinates(molecule)) && !(GeometryTools.has3DCoordinates(molecule)))) {

			StructureDiagramGenerator sdg = new StructureDiagramGenerator();
			sdg.setUseTemplates(false);
			if (!ConnectivityChecker.isConnected(molecule)) {
				IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(molecule);
				molecule = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
				for (int i = 0; i < set.getAtomContainerCount(); i++) {
					sdg.setMolecule(set.getAtomContainer(i), clone);
					sdg.generateCoordinates();
					molecule.add(sdg.getMolecule());
				}
			} else {
				sdg.setMolecule(molecule, clone);
				sdg.generateCoordinates();
				molecule = sdg.getMolecule();
			}
		}

		return molecule;
	}

	public static IAtomContainer calculateCoordinates(IAtomContainer molecule, final boolean force) throws CDKException {
		return calculateCoordinates(molecule, force, false);
	}

	/**
	 * Calculates the InChI string and sets it as property of the CDK molecule.
	 * 
	 * @param molecule the CDK molecule
	 * @param override override existing InChI
	 */
	public synchronized static void calculateInChI(final IAtomContainer molecule, final boolean override) {

		if (molecule.getProperty(CDKConstants.INCHI) == null || override) {

			try {
				InChIGenerator igg = ig.getInChIGenerator(molecule);
				molecule.setProperty(CDKConstants.INCHI, igg.getInchi());
			} catch (CDKException e) {
				try {
					molecule.setProperty(CDKConstants.INCHI, SG.create(molecule));
				} catch (CDKException e1) {
					molecule.setProperty(CDKConstants.INCHI, "");
				}
			}
		}
	}

	/**
	 * Calculates the SMILES string and sets it as property of the CDK molecule.
	 * 
	 * @param molecule the CDK molecule
	 * @param override override existing SMILES
	 * @return the SMILES string
	 */
	public static String calculateSmiles(final IAtomContainer molecule, final int[] sequence, final boolean override) {
		
		String smiles = molecule.getProperty(CDKConstants.SMILES);
		if (override || smiles == null) {
			try {
				smiles = SG.create(molecule, sequence);
			} catch (Exception e) {
				smiles = "";
			}
		}

		return smiles;
	}

	public static String calculateSmiles(final IAtomContainer molecule, final int[] sequence) {
		return calculateSmiles(molecule, sequence, true);
	}

	public static final IAtomContainer parseSDF(String sdf) {

		IAtomContainer molecule = null;
		try {
			MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdf));
			molecule = reader.read(new AtomContainer());
			reader.close();
		} catch (Exception exception) {
			LOGGER.error("SDF format conversion", exception);
		}

		return molecule;
	}

	/**
	 * Calculates the molecule hash.
	 * 
	 * @param molecule the CDK molecule
	 */
	public static long calculateSimpleHash(final IAtomContainer molecule) {

		long hash = 0;
		try {
			hash = GENERATOR.generate(molecule);
		} catch (Exception exception) {

		}

		return hash;
	}

	/**
	 * Calculates the full molecule hash.
	 * 
	 * @param molecule the CDK molecule
	 */
	public static long calculateFullHash(final IAtomContainer molecule) {

		long hash = 0;
		try {
			hash = GENERATOR_FULL.generate(molecule);
		} catch (Exception exception) {

		}

		return hash;
	}

	/**
	 * Returns the max. number of threads available.
	 * 
	 * @return the max. number of threads
	 */
	public static int getMaxNumOfThreads() {
		return ((int) Math.ceil(1.5 * Runtime.getRuntime().availableProcessors()));
	}

	/**
	 * Generates a color palette based on the number of colors required.
	 * 
	 * @param n the number of colors
	 * @return the array of colors
	 */
	public static Color[] generateColors(final int n) {

		Color[] cols = new Color[n];
		for (int i = 0; i < n; i++) {
			cols[i] = Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
		}
		return cols;
	}

	/**
	 * Auto-configures the input column from the data table specification.
	 * 
	 * @param inSpecs the input data table specification
	 * @throws InvalidSettingsException if the input specification is not
	 *         compatible
	 */
	public static String autoConfigure(final DataTableSpec inSpec, String moleculeColumn)
			throws InvalidSettingsException {

		int columnIndex = inSpec.findColumnIndex(moleculeColumn);
		if (columnIndex == -1) {
			String name = null;
			for (DataColumnSpec s : inSpec) {
				if (s.getType().isAdaptable(CDKValue.class)) { // prefer CDK
																// column, use
																// other as
																// fallback
					moleculeColumn = s.getName();
				} else if ((name == null) && s.getType().isAdaptableToAny(CDKNodeUtils.ACCEPTED_VALUE_CLASSES)) {
					moleculeColumn = s.getName();
				}
				// hack to circumvent empty adapter value list map
				if ((name == null) && isAdaptableToAny(s)) {
					moleculeColumn = s.getName();
				}
			}
			if (moleculeColumn == null) {
				throw new InvalidSettingsException("No CDK compatible column in input table");
			}
		}

		return moleculeColumn;
	}

	/**
	 * Checks the data type of the column spec for CDK compatibility.
	 * 
	 * @param s the data column spec
	 * @return if compatible
	 */
	private static boolean isAdaptableToAny(DataColumnSpec s) {

		for (Class<? extends DataValue> cl : CDKNodeUtils.ACCEPTED_VALUE_CLASSES) {
			if (cl == s.getType().getPreferredValueClass()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Auto-configures the input column from the data table specification.
	 * 
	 * @param inSpecs the input data table specification
	 * @throws InvalidSettingsException if the input specification is not
	 *         compatible
	 */
	public static String autoConfigure(final DataTableSpec inSpecs, String column,
			Class<? extends DataValue> columnClass) throws InvalidSettingsException {

		int columnIndex = inSpecs.findColumnIndex(column);
		if (columnIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs) {
				if (spec.getType().isCompatible(columnClass)) {
					columnIndex = i;
					column = spec.getName();
				}
				i++;
			}

			if (columnIndex == -1)
				throw new InvalidSettingsException("Column '" + column + "' does not exist.");
		}

		if (!inSpecs.getColumnSpec(columnIndex).getType().isCompatible(columnClass))
			throw new InvalidSettingsException("Column '" + column + "' does not contain " + columnClass.getName()
					+ " cells");

		return column;
	}
}
