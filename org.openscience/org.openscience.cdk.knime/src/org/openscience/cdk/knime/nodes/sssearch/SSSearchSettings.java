/*
 * Copyright (C) 2003 - 2016 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.nodes.sssearch;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * This class holds all necessary settings for the substructure search node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SSSearchSettings implements CDKSettings {

	private String smiles;
	private String m_molColumnName;
	private boolean highlight = false;
	private boolean charge = false;
	private boolean exactMatch = false;

	/**
	 * Gets the SDF string of the molecules.
	 * 
	 * @return the sdf the Smiles string
	 */
	public final String getSmiles() {
		return smiles;
	}

	/**
	 * Sets the SMILES string of the molecules.
	 * 
	 * @param sdf the smiles to set
	 */
	public final void setSmiles(String smiles) {
		this.smiles = smiles;
	}

	/**
	 * Returns the name of the column containing the molecules.
	 * 
	 * @return the molecules' column name
	 */
	public String targetColumn() {
		return m_molColumnName;
	}

	/**
	 * Sets the name of the column containing the molecules.
	 * 
	 * @param colName the molecules' column name
	 */
	public void targetColumn(final String colName) {
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
	 * Returns if charge should be matched.
	 * 
	 * @return if charge to be matched
	 */
	public final boolean isCharge() {
		return charge;
	}

	/**
	 * Sets if stereo should be matched exactly.
	 * 
	 * @param exactMatch if stereo should be matched exactly
	 */
	public final void setExactMatch(boolean exactMatch) {
		this.exactMatch = exactMatch;
	}
	
	/**
	 * Returns if stereo should be matched exactly.
	 * 
	 * @return if stereo should be matched exactly
	 */
	public final boolean isExactMatch() {
		return exactMatch;
	}

	/**
	 * Sets charge should be matched.
	 * 
	 * @param charge if charge to be considered
	 */
	public final void setCharge(boolean charge) {
		this.charge = charge;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumnName);
		settings.addString("fragments", smiles);
		settings.addBoolean("highlight", highlight);
		settings.addBoolean("charge", charge);
		settings.addBoolean("stereo", exactMatch);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are
	 *         available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumnName = settings.getString("molColumn");
		smiles = settings.getString("fragments");
		highlight = settings.getBoolean("highlight");
		charge = settings.getBoolean("charge");
		exactMatch = settings.getBoolean("stereo");
	}
}
