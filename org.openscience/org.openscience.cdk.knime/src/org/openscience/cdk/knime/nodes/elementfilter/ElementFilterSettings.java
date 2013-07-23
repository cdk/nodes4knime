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
package org.openscience.cdk.knime.nodes.elementfilter;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * Settings for the "ElementFilterNode".
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ElementFilterSettings implements CDKSettings {

	private String molColumnName;
	private String elements = "C,H,N,O,P,S";

	/**
	 * Gets the name of the CDK column.
	 * 
	 * @return the column name
	 */
	public final String targetColumn() {
		return molColumnName;
	}

	/**
	 * Sets the name of the CDK column.
	 * 
	 * @param molColumnName a column name
	 */
	public final void targetColumn(String molColumnName) {
		this.molColumnName = molColumnName;
	}

	/**
	 * Gets the comma-separated element string.
	 * 
	 * @return the element string
	 */
	public final String getElements() {
		return elements;
	}

	/**
	 * Sets the comma-separated element string.
	 * 
	 * @param elements
	 */
	public final void setElements(String elements) {
		this.elements = elements;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColName", molColumnName);
		settings.addString("elements", elements);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		molColumnName = settings.getString("molColName");
		elements = settings.getString("elements");
	}
}
