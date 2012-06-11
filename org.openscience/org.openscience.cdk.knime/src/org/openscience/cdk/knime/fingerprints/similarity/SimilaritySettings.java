/*
 * Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
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

	public enum FingerprintTypes {
		Standard, Extended, EState, MACCS, Pubchem
	}

	private String m_fingerprintColumn = null;
	private String m_fingerprintRefColumn = null;
	private AggregationMethod m_aggregation = AggregationMethod.Average;

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
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 * @throws InvalidSettingsException if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_fingerprintColumn = settings.getString("molColumn");
		m_fingerprintRefColumn = settings.getString("molRefColumn");
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
		settings.addString("aggregationMethod", m_aggregation.toString());
	}
}
