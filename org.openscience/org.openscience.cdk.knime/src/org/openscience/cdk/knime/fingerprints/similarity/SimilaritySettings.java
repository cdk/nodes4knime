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
package org.openscience.cdk.knime.fingerprints.similarity;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the similarity node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilaritySettings {

	/** Enum for the different aggregation methods. */
	public enum AggregationMethod {
		Minimum, Maximum, Average
	}

	/** Enum for the different fingerprint types. */
	public enum FingerprintTypes {
		Standard, Extended, EState, MACCS, Pubchem
	}

	/** Enum for the two return type options. */
	public enum ReturnType {
		String, Collection
	}

	private String m_fingerprintColumn = null;
	private String m_fingerprintRefColumn = null;
	private AggregationMethod m_aggregation = AggregationMethod.Average;
	private ReturnType returnType = ReturnType.String;

	/**
	 * Returns the name of the column that holds the fingerprints.
	 * 
	 * @return a column name
	 */
	public String fingerprintColumn() {

		return m_fingerprintColumn;
	}

	/**
	 * Sets the name of the column that holds the fingerprints.
	 * 
	 * @param columnName a column name
	 */
	public void fingerprintColumn(final String columnName) {

		m_fingerprintColumn = columnName;
	}

	/**
	 * Returns the name of the column that holds the reference fingerprints.
	 * 
	 * @return a column name
	 */
	public String fingerprintRefColumn() {

		return m_fingerprintRefColumn;
	}

	/**
	 * Sets the name of the column that holds the reference fingerprints.
	 * 
	 * @param columnName a column name
	 */
	public void fingerprintRefColumn(final String columnName) {

		m_fingerprintRefColumn = columnName;
	}

	/**
	 * Returns the aggregation method that should be used.
	 * 
	 * @return the aggregation method
	 */
	public AggregationMethod aggregationMethod() {

		return m_aggregation;
	}

	/**
	 * Sets the aggregation method that should be used.
	 * 
	 * @param type the aggregation method
	 */
	public void aggregationMethod(final AggregationMethod aggregation) {

		m_aggregation = aggregation;
	}

	/**
	 * Returns the return type that should be used.
	 * 
	 * @return the return type
	 */
	public ReturnType returnType() {

		return returnType;
	}

	/**
	 * Sets the return type that should be used.
	 * 
	 * @param returnType the return type
	 */
	public void returnType(final ReturnType returnType) {

		this.returnType = returnType;
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 * @throws InvalidSettingsException if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_fingerprintColumn = settings.getString("molColumn");
		m_fingerprintRefColumn = settings.getString("molRefColumn");
		returnType = ReturnType.valueOf(settings.getString("returnType"));
		m_aggregation = AggregationMethod.valueOf(settings.getString("aggregationMethod"));
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_fingerprintColumn);
		settings.addString("molRefColumn", m_fingerprintRefColumn);
		settings.addString("returnType", returnType.toString());
		settings.addString("aggregationMethod", m_aggregation.toString());
	}
}
