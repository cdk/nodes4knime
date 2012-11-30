/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
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
package org.openscience.cdk.knime.sssearch;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds all necessary settings for the substructure search node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SSSearchSettings {

	private String sdf;
	private String m_molColumnName;
	private boolean highlight = false;

	
	/**
	 * Gets the SDF string of the molecules.
	 * 
	 * @return the sdf the SDF string
	 */
	public final String getSdf() {
	
		return sdf;
	}

	
	/**
	 * Sets the SDF string of the molecules.
	 * 
	 * @param sdf the sdf to set
	 */
	public final void setSdf(String sdf) {
	
		this.sdf = sdf;
	}

	/**
	 * Returns the name of the column containing the molecules.
	 * 
	 * @return the molecules' column name
	 */
	public String molColName() {

		return m_molColumnName;
	}

	/**
	 * Sets the name of the column containing the molecules.
	 * 
	 * @param colName the molecules' column name
	 */
	public void molColName(final String colName) {

		m_molColumnName = colName;
	}
	
	/**
	 * Returns if the substructure should be highlighted.
	 * 
	 * @return if highlighted
	 */
	public final boolean isHighlight() {
	
		return highlight;
	}
	
	/**
	 * Sets if the substructure should be highlighted.
	 * 
	 * @param highlight if highlighted
	 */
	public final void setHighlight(boolean highlight) {
	
		this.highlight = highlight;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumnName);
		settings.addString("fragments", sdf);
		settings.addBoolean("highlight", highlight);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumnName = settings.getString("molColumn");
		sdf = settings.getString("fragments");
		highlight = settings.getBoolean("highlight");
	}
}
