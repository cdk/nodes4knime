/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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

import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Utility functions for CDK object standardisation.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKNodeUtils {

	private static final String SMILES = "smiles";

	private static final CDKHydrogenAdder hydra = CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance());
	private static final StructureDiagramGenerator sdg = new StructureDiagramGenerator();
	private static final SmilesGenerator sg = new SmilesGenerator(true);

	/**
	 * Gets the standardised CDK KNIME molecule with implicit hydrogens and detected aromaticity.
	 * 
	 * @param molecule the untyped CDK molecule
	 * @param calcCoordinates whether to calculate 2D coordinates
	 * @throws CDKException description of the exception
	 */
	public synchronized static void getStandardMolecule(IAtomContainer molecule) throws CDKException {

		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
		hydra.addImplicitHydrogens(molecule);
		CDKHueckelAromaticityDetector.detectAromaticity(molecule);
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
			SMSDNormalizer.convertImplicitToExplicitHydrogens(clone);
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
	 * @throws CDKException description of the exception
	 */
	public synchronized static IAtomContainer calculateCoordinates(IAtomContainer molecule, boolean force)
			throws CDKException {

		if (force || (GeometryTools.has2DCoordinatesNew(molecule) != 2)) {
			if (!ConnectivityChecker.isConnected(molecule)) {
				IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(molecule);
				molecule = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
				for (int i = 0; i < set.getAtomContainerCount(); i++) {
					sdg.setMolecule(set.getAtomContainer(i), true);
					sdg.generateCoordinates();
					molecule.add(sdg.getMolecule());
				}
			} else {
				sdg.setMolecule(molecule, true);
				sdg.generateCoordinates();
				molecule = sdg.getMolecule();
			}
		}

		return molecule;
	}

	/**
	 * Calculates the SMILES string and sets it as property of the CDK molecule.
	 * 
	 * @param molecule the CDK molecule
	 * @throws CDKException description of the exception
	 */
	public synchronized static void calculateSmiles(IAtomContainer molecule) {

		if (molecule.getProperty(SMILES) == null) {
			String smiles = sg.createSMILES(molecule);
			molecule.setProperty(SMILES, smiles);
		}
	}
}
