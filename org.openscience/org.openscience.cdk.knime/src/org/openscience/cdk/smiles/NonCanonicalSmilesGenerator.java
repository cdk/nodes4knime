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
package org.openscience.cdk.smiles;

import org.openscience.cdk.interfaces.IAtomContainer;

import uk.ac.ebi.beam.Functions;
import uk.ac.ebi.beam.Graph;

public class NonCanonicalSmilesGenerator {

	private final CDKToBeam converter = new CDKToBeam(true);;

	/**
	 * Generate SMILES for the provided {@code molecule}.
	 * 
	 * @param molecule The molecule to evaluate
	 * @param sequence The atom sequence
	 * @return the SMILES string
	 */
	public String createSMILES(IAtomContainer molecule, int[] sequence) {

		try {
			Graph g = converter.toBeamGraph(molecule);
	
			// collapse() removes redundant hydrogen labels
			g = Functions.collapse(g);
	
			// apply the CANON labelling
			// g = Functions.canonicalize(g, labels(molecule));
	
			// collapse() removes redundant hydrogen labels
			if (sequence == null || sequence.length == 0) {
				return g.toSmiles();
			} else {
				return g.toSmiles(sequence);
			}
		} catch (Exception exception) {
			return null;
		}
	}

	/**
	 * Generate SMILES for the provided {@code molecule}.
	 * 
	 * @param molecule The molecule to evaluate
	 * @return the SMILES string
	 */
	public String createSMILES(IAtomContainer molecule) {

		return createSMILES(molecule, new int[0]);
	}
}
