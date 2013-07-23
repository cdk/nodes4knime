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
package org.openscience.cdk.knime.nodes.opsin;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the "OpsinNameConverter" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
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
