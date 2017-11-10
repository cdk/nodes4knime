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
package org.openscience.cdk.knime.nodes.fingerprints;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * This class holds the settings for the fingerprint node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class FingerprintSettings implements CDKSettings {

	/** Enum for the different fingerprint types. */
	public enum FingerprintTypes {
		Standard, Extended, EState, MACCS, Pubchem, Circular
	}
	
	/** Enum for the different circular fingerprint classes. */
	public enum FingerprintClasses {
		ECFP0(1), ECFP2(2), ECFP4(3), ECFP6(4), FCFP0(5), FCFP2(6), FCFP4(7), FCFP6(8);
		
		private final int value;
		FingerprintClasses(int value) {
			this.value = value;
		}
		
		int getValue() {
			return value;
		}
	}

	private String m_molColumn = null;

	private FingerprintTypes m_fingerprintType = FingerprintTypes.Standard;
	private FingerprintClasses fingerprintClass = FingerprintClasses.ECFP6;

	/**
	 * Returns the name of the column that holds the molecules.
	 * 
	 * @return a column name
	 */
	public String targetColumn() {
		return m_molColumn;
	}

	/**
	 * Sets the name of the column that holds the molecules.
	 * 
	 * @param columnName a column name
	 */
	public void targetColumn(final String columnName) {
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
	 * Returns the class of the circular fingerprint that should be used.
	 * 
	 * @return the class of the circular fingerprint
	 */
	public FingerprintClasses fingerprintClass() {
		return fingerprintClass;
	}

	/**
	 * Sets the class of the circular fingerprint that should be used.
	 * 
	 * @param clazz the clazz of the circular fingerprint
	 */
	public void fingerprintClass(final FingerprintClasses clazz) {
		fingerprintClass = clazz;
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
		fingerprintClass = FingerprintClasses.valueOf(settings.getString("fingerprintClass",
				FingerprintClasses.ECFP6.toString()));
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
		fingerprintClass = FingerprintClasses.valueOf(settings.getString("fingerprintClass"));
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumn);
		settings.addString("fingerprintType", m_fingerprintType.toString());
		settings.addString("fingerprintClass", fingerprintClass.toString());
	}
}
