/*
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
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
 * ---------------------------------------------------------------------
 * 
 * History 16.09.2008 (thor): created
 */
package org.openscience.cdk.knime.convert.cdk2molecule;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the CDK->Molecule node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDK2MoleculeSettings {

	/** Enum with all supported formats. */
	public enum Format {
		/** MDL SDF. */
		SDF,
		/** Smiles. */
		Smiles,
		/** Tripos Mol2. */
		Mol2,
		/** Chemical Markup Language. */
		CML
	}

	private String m_colName;

	private Format m_destFormat = Format.SDF;

	private boolean m_replaceColumn = true;

	private String m_newColName;

	/**
	 * Sets the name of the source molecule column.
	 * 
	 * @param colName a column name
	 */
	public void columnName(final String colName) {

		m_colName = colName;
	}

	/**
	 * Returns the name of the source molecule column.
	 * 
	 * @return a column name
	 */
	public String columnName() {

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
	 * Sets the desired destination format.
	 * 
	 * @param f the destination format
	 */
	public void destFormat(final Format f) {

		m_destFormat = f;
	}

	/**
	 * Returns the desired destination format.
	 * 
	 * @return the destination format
	 */
	public Format destFormat() {

		return m_destFormat;
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
		m_destFormat = Format.valueOf(settings.getString("destFormat"));
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
		m_destFormat = Format.valueOf(settings.getString("destFormat", Format.SDF.name()));
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
		settings.addString("destFormat", m_destFormat.name());
	}
}
