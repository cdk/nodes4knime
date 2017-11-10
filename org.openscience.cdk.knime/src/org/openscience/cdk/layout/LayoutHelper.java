/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.layout;

import org.openscience.cdk.interfaces.IAtomContainer;

public class LayoutHelper {

	public static void adjustStereo(IAtomContainer molecule) throws IllegalArgumentException {
		
		// correct double-bond stereo, this changes the layout and in reality 
        // should be done during the initial placement
        CorrectGeometricConfiguration.correct(molecule);

        // assign up/down labels, this doesn't not alter layout and could be
        // done on-demand (e.g. when writing a MDL Molfile)
        NonplanarBonds.assign(molecule);
        
        if (molecule == null) {
        	throw new IllegalArgumentException("Stereo perception failed.");
        }
	}
}
