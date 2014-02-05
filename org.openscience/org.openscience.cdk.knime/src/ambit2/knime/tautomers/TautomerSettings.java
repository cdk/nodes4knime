/*
 * Copyright (c) 2014, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package ambit2.knime.tautomers;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * CDK Settings class for AMBIT's tautomer generator node.
 * 
 * @author Stephan Beisken, EMBL-EBI
 */
public class TautomerSettings implements CDKSettings {

	// name of the CDK molecule column
	private String columnName;
	// execution mode of the tautomer worker
	private TautomerWorker.Mode mode = TautomerWorker.Mode.BEST_REPLACE;

	/**
	 * Returns the name of the column containing the molecules.
	 * 
	 * @return the molecules' column name
	 */
	public String targetColumn() {
		return columnName;
	}

	/**
	 * Sets the name of the column containing the molecules.
	 * 
	 * @param columnName the molecules' column name
	 */
	public void targetColumn(final String columnName) {
		this.columnName = columnName;
	}

	/**
	 * Returns the execution mode of the tautomer worker.
	 * 
	 * @return the execution mode
	 */
	public TautomerWorker.Mode mode() {
		return mode;
	}

	/**
	 * Sets the execution mode of the tautomer worker.
	 * 
	 * @param mode the execution mode
	 */
	public void mode(final TautomerWorker.Mode mode) {
		this.mode = mode;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("Molecule", columnName);
		settings.addInt("Generate", mode.ordinal());
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		columnName = settings.getString("Molecule");
		mode = TautomerWorker.Mode.values()[settings.getInt("Generate")];
	}
}
