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
package org.openscience.cdk.knime.nodes.descriptors.whim3d;

/**
 * Constants for the weighting schemes used by the WHIM descriptor.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public enum Whim3dSchemes {

	UNITY_WEIGHTS("Unit Weights", "unity"), ATOMIC_MASSES("Atomic Masses", "mass"), ATOMIC_POLARIZABILITIES(
			"Atomic Polarizabilities", "polar"), VdW_VOLUMES("VdW Volumes", "volume"), ATOMIC_ELECTRONEGATIVITIES(
			"Atomic Electronegativities", "eneg");

	private String title;
	private String parameterName;

	private Whim3dSchemes(String title, String parameterName) {

		this.title = title;
		this.parameterName = parameterName;
	}

	/**
	 * @return the column title for the weighing scheme
	 */
	public synchronized String getTitle() {
		return title;
	}

	/**
	 * @return the name of the parameter passed to the descriptor
	 */
	public synchronized String getParameterName() {
		return parameterName;
	}
}
