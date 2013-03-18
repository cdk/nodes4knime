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
package org.openscience.cdk.knime;

import java.awt.Color;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import uk.ac.ebi.mdk.prototype.hash.HashGenerator;
import uk.ac.ebi.mdk.prototype.hash.HashGeneratorMaker;

/**
 * Utility functions for CDK object standardisation.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKNodeUtils {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKNodeUtils.class);
	private static final HashGenerator<Integer> GENERATOR = new HashGeneratorMaker().withDepth(8).charged().chiral()
			.isotopic().radicals().withBondOrderSum().nullable().build();
	private static final SmilesGenerator SG = new SmilesGenerator(true);

	private static InChIGeneratorFactory ig;
	static {
		try {
			ig = InChIGeneratorFactory.getInstance();
			ig.setIgnoreAromaticBonds(true);
		} catch (CDKException e) {
			LOGGER.error("Failed to load the InChIGeneratorFactory.");
		}
	}

	/**
	 * Gets the standardised CDK KNIME molecule with implicit hydrogens and detected aromaticity.
	 * 
	 * @param molecule the untyped CDK molecule
	 * @param calcCoordinates whether to calculate 2D coordinates
	 * @throws CDKException description of the exception
	 */
	public static void getStandardMolecule(IAtomContainer molecule) throws CDKException {

		try {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
			CDKHydrogenAdder hydra = CDKHydrogenAdder.getInstance(molecule.getBuilder());
			hydra.addImplicitHydrogens(molecule);
			hydra = null;
			if (ConnectivityChecker.isConnected(molecule)) {
				CDKHueckelAromaticityDetector.detectAromaticity(molecule);
			} else {
				IAtomContainerSet moleculeSet = ConnectivityChecker.partitionIntoMolecules(molecule);
				molecule.removeAllElements();
				for (IAtomContainer mol : moleculeSet.atomContainers()) {
					CDKHueckelAromaticityDetector.detectAromaticity(mol);
					molecule.add(mol);
				}
				moleculeSet = null;
			}
		} catch (IllegalAccessError error) {
			throw new CDKException("Illegal Access Error - QueryChemObject: " + error.getMessage());
		} catch (Exception exception) {
			exception.printStackTrace();
			throw new CDKException("Exception while standardizing: " + exception.getMessage());
		}
	}

	/**
	 * Gets the clone of the CDK KNIME input molecule with all hydrogens set as explicit hydrogens.
	 * 
	 * @param molecule the input CDK molecule
	 * @return the CDK molecule clone with explicit hydrogens
	 * @throws CDKException description of the exception
	 */
	public synchronized static IAtomContainer getExplicitClone(IAtomContainer molecule) throws CDKException {

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
	 * Calculates 2D coordinates for the CDK molecule. If 'forced', the coordinates will be generated even if the
	 * molecule has 2D coordinates already.
	 * 
	 * @param molecule the CDK molecule
	 * @param force whether to force the calculation of 2D coordinates
	 * @param clone whether to clone the CDK molecule
	 * @throws CDKException description of the exception
	 */
	public static IAtomContainer calculateCoordinates(IAtomContainer molecule, boolean force, boolean clone)
			throws CDKException {

		if (force || (!(GeometryTools.has2DCoordinates(molecule)) && !(GeometryTools.has3DCoordinates(molecule)))) {

			StructureDiagramGenerator sdg = new StructureDiagramGenerator();
			if (!ConnectivityChecker.isConnected(molecule)) {
				IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(molecule);
				molecule = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
				for (int i = 0; i < set.getAtomContainerCount(); i++) {
					sdg.setMolecule(set.getAtomContainer(i), true);
					sdg.generateCoordinates();
					molecule.add(sdg.getMolecule());
				}
				set = null;
			} else {
				sdg.setMolecule(molecule, clone);
				sdg.generateCoordinates();
				molecule = sdg.getMolecule();
			}
			sdg = null;
		}

		return molecule;
	}

	/**
	 * Calculates the InChI string and sets it as property of the CDK molecule.
	 * 
	 * @param molecule the CDK molecule
	 * @param override override existing InChI
	 */
	public synchronized static void calculateInChI(IAtomContainer molecule, boolean override) {

		if (molecule.getProperty(CDKConstants.INCHI) == null || override) {

			try {
				InChIGenerator igg = ig.getInChIGenerator(molecule);
				molecule.setProperty(CDKConstants.INCHI, igg.getInchi());
			} catch (CDKException e) {
				molecule.setProperty(CDKConstants.INCHI, SG.createSMILES(molecule));
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
	public synchronized static String calculateSmiles(IAtomContainer molecule, boolean override) {

		String smiles = "";
		if (molecule.getProperty(CDKConstants.SMILES) == null || override) {

			SmilesGenerator sg = new SmilesGenerator();
			sg.setUseAromaticityFlag(true);
			smiles = sg.createSMILES(molecule);
			if (smiles == null) {
				smiles = "";
			}
		}

		return smiles;
	}

	public static void calculateHash(IAtomContainer molecule) {

		int hash;
		try {
			hash = GENERATOR.generate(molecule);
		} catch (Exception exception) {
			hash = 0;
		}
		molecule.setProperty(CDKConstants.MAPPED, hash);
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
	public static Color[] generateColors(int n) {

		Color[] cols = new Color[n];
		for (int i = 0; i < n; i++) {
			cols[i] = Color.getHSBColor((float) i / (float) n, 0.85f, 1.0f);
		}
		return cols;
	}

	/**
	 * Configures the input column.
	 * 
	 * @param inSpec the input data table specification
	 * @param columnName the input column name
	 * @param cellClass the desired cell class
	 * @return the input or autoconfigured column name
	 * @throws InvalidSettingsException if no column in the data table specification is compatible with the desired cell
	 *         class
	 */
	public static String getColumn(final DataTableSpec inSpec, String columnName, Class<? extends DataValue> cellClass)
			throws InvalidSettingsException {

		int columnIndex = inSpec.findColumnIndex(columnName);
		if (columnIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpec) {
				if (spec.getType().isCompatible(cellClass)) {
					columnIndex = i;
					columnName = spec.getName();
				}
				i++;
			}

			if (columnIndex == -1)
				throw new InvalidSettingsException("Column '" + columnName + "' does not exist.");
		}

		if (!inSpec.getColumnSpec(columnIndex).getType().isCompatible(cellClass))
			throw new InvalidSettingsException("Column '" + columnName + "' does not contain " + cellClass.getName()
					+ " cells");

		return columnName;
	}
}
