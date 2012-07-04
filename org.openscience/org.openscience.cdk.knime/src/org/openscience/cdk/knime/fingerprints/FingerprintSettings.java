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
package org.openscience.cdk.knime.fingerprints;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the fingerprint node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class FingerprintSettings {

	/** Enum for the different fingerprint types. */
	public enum FingerprintTypes {
		Standard, Extended, EState, MACCS, Pubchem
	}

	private String m_molColumn = null;

	private FingerprintTypes m_fingerprintType = FingerprintTypes.Standard;

	/**
	 * Returns the name of the column that holds the molecules.
	 * 
	 * @return a column name
	 */
	public String molColumn() {

		return m_molColumn;
	}

	/**
	 * Sets the name of the column that holds the molecules.
	 * 
	 * @param columnName a column name
	 */
	public void molColumn(final String columnName) {

		m_molColumn = columnName;
	}

	/**
	 * Returns the type of fingerprints that should be generated.
	 * 
	 * @return the type of fingerprint
	 */
	public FingerprintTypes fingerprintType() {

		return m_fingerprintType;
	}

	/**
	 * Sets the type of fingerprints that should be generated.
	 * 
	 * @param type the type of fingerprint
	 */
	public void fingerprintType(final FingerprintTypes type) {

		m_fingerprintType = type;
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void loadSettingsForDialog(final NodeSettingsRO settings) {

		m_molColumn = settings.getString("molColumn", null);
		m_fingerprintType = FingerprintTypes.valueOf(settings.getString("fingerprintType",
				FingerprintTypes.Standard.toString()));
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 * @throws InvalidSettingsException if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumn = settings.getString("molColumn");
		m_fingerprintType = FingerprintTypes.valueOf(settings.getString("fingerprintType"));
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumn);
		settings.addString("fingerprintType", m_fingerprintType.toString());
	}
}
