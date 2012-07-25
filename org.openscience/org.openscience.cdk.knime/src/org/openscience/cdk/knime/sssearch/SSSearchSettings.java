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
 */
public class SSSearchSettings {

	private String[] m_smilesFragments;
	private String m_molColumnName;

	/**
	 * Returns Smiles strings containing the fragment.
	 * 
	 * @return the fragments in Smiles
	 */
	public String[] smilesFragments() {

		return m_smilesFragments;
	}

	/**
	 * Sets the Smiles strings containing the fragments.
	 * 
	 * @param cml the fragments in Smiles
	 */
	public void smilesFragments(final String... cml) {

		m_smilesFragments = cml;
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
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumnName);
		settings.addStringArray("fragments", m_smilesFragments);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumnName = settings.getString("molColumn");
		m_smilesFragments = settings.getStringArray("fragments");
	}
}
