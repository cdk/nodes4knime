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
package org.openscience.cdk.knime.core;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Abstract node model for the CDK extension.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public abstract class CDKNodeModel extends NodeModel {

	// the molecule column index
	protected int columnIndex;
	// the CDK settings
	protected final CDKSettings settings;

	/**
	 * Constructor for the CDK node model.
	 * 
	 * @param nrDataIns the number of input data ports
	 * @param nrDataOuts the number of output data ports
	 * @param settings the CDK specific settings object
	 */
	public CDKNodeModel(int nrDataIns, int nrDataOuts, CDKSettings settings) {
		super(nrDataIns, nrDataOuts);
		this.settings = settings;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger cr = createColumnRearranger(inData[0].getDataTableSpec());
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], cr, exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// auto-configures the settings for an available molecule column if none is set
		autoConfigure(inSpecs);

		// creates the column rearranger -- does the heavy lifting for adapter cells
		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		this.settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// nothing to do
	}

	/**
	 * Auto-configures the input column from the data table specification.
	 * 
	 * @param inSpecs the input data table specification
	 * @throws InvalidSettingsException if the input specification is not compatible
	 */
	protected void autoConfigure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (settings.targetColumn() == null) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isAdaptable(CDKValue.class)) { // prefer CDK column, use other as fallback
					name = s.getName();
				} else if ((name == null) && s.getType().isAdaptableToAny(CDKNodeUtils.ACCEPTED_VALUE_CLASSES)) {
					name = s.getName();
				}
				
				// hack to circumvent empty adapter value list map
				if ((name == null) && isAdaptableToAny(s)) {
					name = s.getName();
				}
			}
			if (name != null) {
				settings.targetColumn(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else {
				throw new InvalidSettingsException("No CDK compatible column in input table");
			}
		}
	}
	
	/**
	 * Checks the data type of the column spec for CDK compatibility.
	 * 
	 * @param s the data column spec
	 * @return if compatible
	 */
	private boolean isAdaptableToAny(DataColumnSpec s) {

		for (Class<? extends DataValue> cl : CDKNodeUtils.ACCEPTED_VALUE_CLASSES) {
			if (cl == s.getType().getPreferredValueClass()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the settings object for a particular class.
	 * 
	 * @param type the settings class
	 * @return the settings object
	 */
	protected <T> T settings(Class<T> type) {
		return type.cast(settings);
	}

	/**
	 * Creates the column rearranger containing the cell factory and output specifications.
	 * 
	 * @param spec the input table specification
	 * @return the column rearranger
	 * @throws InvalidSettingsException if the settings are not compatible
	 */
	protected abstract ColumnRearranger createColumnRearranger(final DataTableSpec spec)
			throws InvalidSettingsException;
}
