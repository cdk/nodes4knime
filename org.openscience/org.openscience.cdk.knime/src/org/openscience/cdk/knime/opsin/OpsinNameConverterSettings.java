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
package org.openscience.cdk.knime.opsin;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the "OpsinNameConverter" Node.
 * 
 * @author Stephan Beisken
 * 
 */
public class OpsinNameConverterSettings {

	private String nameColumn;
	private boolean addCdk;
	private boolean addInChI;
	private boolean addCml;
	private boolean addSmiles;
	private boolean addPng;

	/**
	 * Checks if a CDK molecule is to be generated.
	 * 
	 * @return boolean
	 */
	public boolean isAddCdk() {

		return addCdk;
	}

	/**
	 * Sets if a CDK molecule is to be generated.
	 * 
	 * @param addCdk boolean
	 */
	public void setAddCdk(boolean addCdk) {

		this.addCdk = addCdk;
	}

	/**
	 * Checks if CML is to be retrieved.
	 * 
	 * @return boolean
	 */
	public boolean isAddCml() {

		return addCml;
	}

	/**
	 * Sets if CML is to be retrieved.
	 * 
	 * @param addCml boolean
	 */
	public void setAddCml(boolean addCml) {

		this.addCml = addCml;
	}

	/**
	 * Checks if SMILES is to be retrieved.
	 * 
	 * @return boolean
	 */
	public boolean isAddSmiles() {

		return addSmiles;
	}

	/**
	 * Sets if SMILES is to be retrieved.
	 * 
	 * @param addSmiles boolean
	 */
	public void setAddSmiles(boolean addSmiles) {

		this.addSmiles = addSmiles;
	}

	/**
	 * Checks if PNG image is to be retrieved.
	 * 
	 * @return boolean
	 */
	public boolean isAddPng() {

		return addPng;
	}

	/**
	 * Sets if PNG image is to be retrieved.
	 * 
	 * @param addPng boolean
	 */
	public void setAddPng(boolean addPng) {

		this.addPng = addPng;
	}

	/**
	 * Gets the column name of the input strings.
	 * 
	 * @return the column name
	 */
	public final String getNameColumn() {

		return nameColumn;
	}

	/**
	 * Sets the column name of the input strings.
	 * 
	 * @param nameColumn
	 */
	public final void setNameColumn(String nameColumn) {

		this.nameColumn = nameColumn;
	}

	/**
	 * Checks if InChI is to be retrieved.
	 * 
	 * @return boolean
	 */
	public final boolean isAddInChI() {

		return addInChI;
	}

	/**
	 * Sets if InChI is to be retrieved.
	 * 
	 * @param addInChI boolean
	 */
	public final void setAddInChI(boolean addInChI) {

		this.addInChI = addInChI;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("nameColumn", nameColumn);
		settings.addBoolean("addCdk", addCdk);
		settings.addBoolean("addInChI", addInChI);
		settings.addBoolean("addSmiles", addSmiles);
		settings.addBoolean("addPng", addPng);
		settings.addBoolean("addCml", addCml);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		nameColumn = settings.getString("nameColumn");
		addCdk = settings.getBoolean("addCdk");
		addInChI = settings.getBoolean("addInChI");
		addSmiles = settings.getBoolean("addSmiles");
		addPng = settings.getBoolean("addPng");
		addCml = settings.getBoolean("addCml");
	}
}
