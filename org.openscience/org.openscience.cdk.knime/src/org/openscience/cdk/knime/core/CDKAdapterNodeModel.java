/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
import java.util.HashMap;
import java.util.Map;

import org.knime.chem.types.InchiValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RWAdapterValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Abstract adapter node model for the CDK extension.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public abstract class CDKAdapterNodeModel extends NodeModel {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(CDKAdapterNodeModel.class);

	protected int columnIndex;
	protected final CDKSettings settings;

	protected int maxParallelWorkers;
	protected int maxQueueSize;

	/**
	 * Creates a new adapter node model.
	 * 
	 * @param nrInDataPorts the number of in-ports
	 * @param nrOutDataPorts the number of out-ports
	 * @param settings an CDK settings instance
	 */
	protected CDKAdapterNodeModel(final int nrInDataPorts, final int nrOutDataPorts, final CDKSettings settings) {
		super(nrInDataPorts, nrOutDataPorts);
		this.settings = settings;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		maxParallelWorkers = (int) Math.ceil(1.5 * Runtime.getRuntime().availableProcessors());
		maxQueueSize = 10 * maxParallelWorkers;

		BufferedDataTable[] convertedTables = null;
		BufferedDataTable[] resultTables = null;

		try {
			// convert compatible tables containing molecule representations to 'CDK tables'
			convertedTables = convertTables(inData, exec);
			// process the converted tables by adding or replacing columns
			resultTables = process(convertedTables, exec);
		} catch (final Throwable exception) {
			LOGGER.error("Error during table conversion.", exception);
		}

		return resultTables;
	}

	/**
	 * Converts compatible column specs of molecule types.
	 * 
	 * @param inSpec the original input spec
	 * @return the converted output spec
	 */
	protected DataTableSpec[] convertTables(final DataTableSpec[] inSpec) {

		DataTableSpec[] convertedTableSpec = null;

		if (inSpec != null) {
			convertedTableSpec = new DataTableSpec[inSpec.length];

			for (int i = 0; i < inSpec.length; i++) {
				// get spec for the selected molecule column
				convertedTableSpec[i] = inSpec[i];

				if (inSpec[i] != null && inSpec[i].getNumColumns() > 0) {
					// check if the molecule column needs conversion
					if (needsConversion(inSpec[i])) {
						final ColumnRearranger rearranger = new ColumnRearranger(inSpec[i]);
						final DataCellTypeConverter converter = CDKTypeConverter
								.createConverter(inSpec[i], columnIndex);
						rearranger.ensureColumnIsConverted(converter, columnIndex);
						convertedTableSpec[i] = rearranger.createSpec();
					}
				}
			}
		}

		return convertedTableSpec;
	}

	/**
	 * Converts data tables with compatible molecule types to data tables with CDK molecule type(s).
	 * 
	 * @param inData the original table data
	 * @param exec the execution context
	 * @return the converted tables
	 * @throws Exception if an error during conversion occurred
	 */
	protected BufferedDataTable[] convertTables(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		BufferedDataTable[] convertedTables = null;

		if (inData != null) {
			convertedTables = new BufferedDataTable[inData.length];

			final Map<Integer, ColumnRearranger> mapColumnRearrangers = new HashMap<Integer, ColumnRearranger>();
			for (int i = 0; i < inData.length; i++) {
				convertedTables[i] = inData[i];

				if (inData[i] != null && inData[i].getDataTableSpec().getNumColumns() > 0) {
					// check if the molecule column needs conversion
					if (needsConversion(inData[i].getDataTableSpec())) {
						final DataTableSpec tableSpec = inData[i].getDataTableSpec();
						// get adapter for the molecule type
						final DataCellTypeConverter converter = CDKTypeConverter
								.createConverter(tableSpec, columnIndex);
						final ColumnRearranger rearranger = new ColumnRearranger(tableSpec);
						// add dummy cell factory to the rearranger to avoid failing assert statements
						rearranger.append(new SingleCellFactory(true, new DataColumnSpecCreator(DataTableSpec
								.getUniqueColumnName(tableSpec, "EmptyCells"), IntCell.TYPE).createSpec()) {

							private final DataCell EMPTY_CELL = DataType.getMissingCell();

							@Override
							public DataCell getCell(final DataRow row) {
								if (row != null) {
									final DataCell cellConverted = row.getCell(columnIndex);
									if (cellConverted instanceof MissingCell) {
										final String strError = ((MissingCell) cellConverted).getError();
										if (strError != null) {
											LOGGER.warn("Auto conversion in row '" + row.getKey().getString()
													+ "' failed - Using empty cell.");
										}
									}
								}
								// dummy cell
								return EMPTY_CELL;
							}
						});
						rearranger.ensureColumnIsConverted(converter, columnIndex);
						mapColumnRearrangers.put(i, rearranger);
					}
				}
			}

			for (int j = 0; j < inData.length; j++) {
				exec.setMessage("Converting input tables for processing");
				final ColumnRearranger rearranger = mapColumnRearrangers.get(j);
				if (rearranger != null) {
					ExecutionMonitor e = exec.createSubProgress(1.0d / 4.0d);
					convertedTables[j] = exec.createColumnRearrangeTable(inData[j], rearranger, e);
					// remove the appended dummy cells from the converted tables
					final DataTableSpec tableSpec = convertedTables[j].getDataTableSpec();
					final ColumnRearranger rearrangerWorkaround = new ColumnRearranger(tableSpec);
					rearrangerWorkaround.remove(tableSpec.getNumColumns() - 1);
					convertedTables[j] = exec.createColumnRearrangeTable(convertedTables[j], rearrangerWorkaround, e);
				}
				exec.setMessage(null);
			}
		}

		return convertedTables;
	}

	/**
	 * Checks if the selected molecule type needs conversion to a CDK type.
	 * 
	 * @param spec the original input spec
	 * @return if conversion needed
	 */
	private boolean needsConversion(final DataTableSpec spec) {

		if (columnIndex >= spec.getNumColumns()) {
			return false;
		}
		DataType type = spec.getColumnSpec(columnIndex).getType();

		if (type.isCompatible(AdapterValue.class)) {
			if (type.isAdaptable(CDKValue.class) || type.isCompatible(CDKValue.class)) {
				return false;
			} else if (type.isCompatible(RWAdapterValue.class) && type.isCompatible(StringValue.class)
					&& type.isCompatible(SmilesValue.class) && type.isCompatible(SdfValue.class)
					&& type.isCompatible(InchiValue.class)) {
				return true;
			} else if (type.isAdaptable(SdfValue.class)) {
				return true;
			} else if (type.isAdaptable(SmilesValue.class)) {
				return true;
			} else if (type.isAdaptable(InchiValue.class)) {
				return true;
			} else if (type.isAdaptable(StringValue.class)) {
				return true;
			}
		} else {
			if (type.isCompatible(CDKValue.class)) {
				return true;
			} else if (type.isCompatible(SdfValue.class)) {
				return true;
			} else if (type.isCompatible(SmilesValue.class)) {
				return true;
			} else if (type.isCompatible(InchiValue.class)) {
				return true;
			} else if (type.isCompatible(StringValue.class) && !type.equals(SmartsCell.TYPE)) {
				return true;
			}
		}

		return false;
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

		columnIndex = inSpecs[0].findColumnIndex(settings.targetColumn());
	}

	/**
	 * Checks the data type of the column spec for CDK compatibility.
	 * 
	 * @param s the data column spec
	 * @return if compatible
	 */
	private boolean isAdaptableToAny(final DataColumnSpec s) {

		for (Class<? extends DataValue> cl : CDKNodeUtils.ACCEPTED_VALUE_CLASSES) {
			if (cl == s.getType().getPreferredValueClass()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
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
	 * Returns the settings object for a particular class.
	 * 
	 * @param type the settings class
	 * @return the settings object
	 */
	protected <T> T settings(final Class<T> type) {
		return type.cast(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected abstract DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException;

	/**
	 * Core method of a node model implementing this abstract class complementing the execute method. Carries out the
	 * actual processing of the input tables and generates the output tables.
	 * 
	 * @param convertedTables the converted (compatible) input tables
	 * @param exec the execution context
	 * @return the resulting output tables
	 * @throws Exception if an error has occurred during execution
	 */
	protected abstract BufferedDataTable[] process(final BufferedDataTable[] convertedTables,
			final ExecutionContext exec) throws Exception;
}
