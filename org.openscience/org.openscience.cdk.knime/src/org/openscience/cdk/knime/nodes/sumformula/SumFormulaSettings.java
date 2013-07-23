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
package org.openscience.cdk.knime.nodes.sumformula;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * Settings for the "SumFormulaNode" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 * 
 */
public class SumFormulaSettings implements CDKSettings {

	private String massColumn;
	private boolean excludeByValidSum;

	/**
	 * Gets the name of the mass containing double column.
	 * 
	 * @return the massColumn
	 */
	public String targetColumn() {
		return massColumn;
	}

	/**
	 * Sets the name of the mass containing double column.
	 * 
	 * @param massColumn the massColumn to set
	 */
	public void targetColumn(String massColumn) {
		this.massColumn = massColumn;
	}

	/**
	 * Returns if unlikely filtered molecular formulas should be exclucded.
	 * 
	 * @return the excludeByValidSum
	 */
	public boolean isExcludeByValidSum() {
		return excludeByValidSum;
	}

	/**
	 * Sets if unlikely filtered molecular formulas should be exclucded.
	 * 
	 * @param excludeByValidSum the excludeByValidSum to set
	 */
	public void setExcludeByValidSum(boolean excludeByValidSum) {
		this.excludeByValidSum = excludeByValidSum;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("massColumn", massColumn);
		settings.addBoolean("exclude", excludeByValidSum);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		massColumn = settings.getString("massColumn");
		excludeByValidSum = settings.getBoolean("exclude");
	}
}
