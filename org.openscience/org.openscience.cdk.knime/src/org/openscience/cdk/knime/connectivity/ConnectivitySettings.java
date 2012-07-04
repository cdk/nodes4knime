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
package org.openscience.cdk.knime.connectivity;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class stores the settings for the connectivity node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivitySettings {

	private String m_molColumnName;

	private boolean m_removeCompleteRow;

	private boolean m_addFragmentColumn;

	/**
	 * Returns the name of the column containing the molecules.
	 * 
	 * @return the molecules' column name
	 */
	public String molColumnName() {

		return m_molColumnName;
	}

	/**
	 * Sets the name of the column containing the molecules.
	 * 
	 * @param colName the molecules' column name
	 */
	public void molColumnName(final String colName) {

		m_molColumnName = colName;
	}

	/**
	 * Returns if rows containing fragmented molecules should be removed completely or only all fragments except the
	 * biggest one.
	 * 
	 * @return <code>true</code> if the whole row should be removed, <code>false</code> if only the small fragments
	 *         should be removed.
	 */
	public boolean removeCompleteRow() {

		return m_removeCompleteRow;
	}

	/**
	 * Sets if rows containing fragmented molecules should be removed completely or only all fragments except the
	 * biggest one.
	 * 
	 * @param removeCompleteRow <code>true</code> if the whole row should be removed, <code>false</code> if only the
	 *        small fragments should be removed.
	 */
	public void removeCompleteRow(final boolean removeCompleteRow) {

		m_removeCompleteRow = removeCompleteRow;
	}

	/**
	 * Returns if a column with all fragments should be added.
	 * 
	 * @return <code>true</code> if a column should be added, <code>false</code> otherwise
	 */
	public boolean addFragmentColumn() {

		return m_addFragmentColumn;
	}

	/**
	 * Sets if a column with all fragments should be added.
	 * 
	 * @param add <code>true</code> if a column should be added, <code>false</code> otherwise
	 */
	public void addFragmentColumn(final boolean add) {

		m_addFragmentColumn = add;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumnName);
		settings.addBoolean("removeCompleteRow", m_removeCompleteRow);
		settings.addBoolean("addFragmentColumn", m_addFragmentColumn);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumnName = settings.getString("molColumn");
		m_removeCompleteRow = settings.getBoolean("removeCompleteRow");
		m_addFragmentColumn = settings.getBoolean("addFragmentColumn", false);
	}
}
