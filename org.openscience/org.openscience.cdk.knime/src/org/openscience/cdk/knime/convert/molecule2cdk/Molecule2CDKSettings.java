/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.convert.molecule2cdk;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * This class holds the settings for the Molecule->CDK node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Molecule2CDKSettings implements CDKSettings {

	private String m_colName;
	private boolean m_replaceColumn = true;
	private String m_newColName;
	private boolean m_generate2D = true;
	private boolean m_force2D = false;
	private boolean convertOrder = false;

	/**
	 * Sets the name of the source molecule column.
	 * 
	 * @param colName a column name
	 */
	public void targetColumn(final String colName) {
		m_colName = colName;
	}

	/**
	 * Returns the name of the source molecule column.
	 * 
	 * @return a column name
	 */
	public String targetColumn() {
		return m_colName;
	}

	/**
	 * Sets if the node should replace the source column or append an additional column.
	 * 
	 * @param b <code>true</code> if the source column should be replaced, <code>false</code> otherwise
	 */
	public void replaceColumn(final boolean b) {
		m_replaceColumn = b;
	}

	/**
	 * Returns if the node should replace the source column or append an additional column.
	 * 
	 * @return <code>true</code> if the source column should be replaced, <code>false</code> otherwise
	 */
	public boolean replaceColumn() {
		return m_replaceColumn;
	}

	/**
	 * Sets the name of the new column that is used if {@link #replaceColumn()} is <code>false</code>.
	 * 
	 * @param name the name of the new column
	 */
	public void newColumnName(final String name) {
		m_newColName = name;
	}

	/**
	 * Returns the name of the new column that is used if {@link #replaceColumn()} is <code>false</code>.
	 * 
	 * @return the name of the new column
	 */
	public String newColumnName() {
		return m_newColName;
	}

	/**
	 * Sets if 2D coordinates should be generated for all converted molecules.
	 * 
	 * @param gen <code>true</code> if they should be generated, <code>false</code> otherwise
	 */
	public void generate2D(final boolean gen) {
		m_generate2D = gen;
	}

	/**
	 * Returns if 2D coordinates should be generated for all converted molecules.
	 * 
	 * @return <code>true</code> if they should be generated, <code>false</code> otherwise
	 */
	public boolean generate2D() {
		return m_generate2D;
	}

	/**
	 * Sets if 2D coordinate generation should be forced even if the molecules already seem to have 2D coordinates.
	 * 
	 * @param force <code>true</code> if they should be generated in any case, <code>false</code> otherwise
	 */
	public void force2D(final boolean force) {
		m_force2D = force;
	}

	/**
	 * Returns if 2D coordinate generation should be forced even if the molecules already seem to have 2D coordinates.
	 * 
	 * @return <code>true</code> if they should be generated in any case, <code>false</code> otherwise
	 */
	public boolean force2D() {
		return m_force2D;
	}

	/**
	 * Returns if bond order of type 4 should be converted to bond orders 1 and 2.
	 * 
	 * @return <code>true</code> if they should be converted, <code>false</code> otherwise
	 */
	public boolean convertOrder() {
		return convertOrder;
	}

	/**
	 * Sets if 2D bond order of type 4 should be converted to bond orders 1 and 2.
	 * 
	 * @param force <code>true</code> if they should be converted, <code>false</code> otherwise
	 */
	public void convertOrder(final boolean conertOrder) {
		this.convertOrder = conertOrder;
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 * @throws InvalidSettingsException if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_colName = settings.getString("colName");
		m_replaceColumn = settings.getBoolean("replaceColumn");
		m_newColName = settings.getString("newColName");
		m_generate2D = settings.getBoolean("generate2D");
		m_force2D = settings.getBoolean("force2D");
		convertOrder = settings.getBoolean("bondOrder");
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void loadSettingsForDialog(final NodeSettingsRO settings) {

		m_colName = settings.getString("colName", null);
		m_replaceColumn = settings.getBoolean("replaceColumn", true);
		m_newColName = settings.getString("newColName", "");
		m_generate2D = settings.getBoolean("generate2D", false);
		m_force2D = settings.getBoolean("force2D", false);
		convertOrder = settings.getBoolean("bondOrder", false);
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("colName", m_colName);
		settings.addBoolean("replaceColumn", m_replaceColumn);
		settings.addString("newColName", m_newColName);
		settings.addBoolean("generate2D", m_generate2D);
		settings.addBoolean("force2D", m_force2D);
		settings.addBoolean("bondOrder", convertOrder);
	}
}
