/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.molprops;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SinglePropertyNodeModel extends ThreadedColAppenderNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SinglePropertyNodeModel.class);

	/** Config key for cdk column. */
	static final String CFG_CDK_COL = "cdkColumn";

	private final SettingsModelString m_cdkColSelModel;
	private final String m_descriptorClassName;

	/**
	 * Inits super with one input, one output.
	 * 
	 * @param descriptorClassName The class name of the CDK descriptor.
	 */
	public SinglePropertyNodeModel(final String descriptorClassName) {

		super(1, 1);
		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
		m_cdkColSelModel = createColSelectorSettingsModel();
		m_descriptorClassName = descriptorClassName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		String name = m_cdkColSelModel.getStringValue();
		if (name == null) {
			for (DataColumnSpec c : inSpecs[0]) {
				if (c.getType().isCompatible(CDKValue.class)) {
					name = c.getName();
				}
			}
			if (name != null) {
				m_cdkColSelModel.setStringValue(name);
				setWarningMessage("Auto configuration: using column \"" + name + "\".");
			} else {
				throw new InvalidSettingsException("No CDK compatible column " + "in input table");
			}
		}
		DataTableSpec outSpec = new DataTableSpec(generateOutputColSpec(inSpecs[0]));
		DataTableSpec[] outSpecs = new DataTableSpec[] { new DataTableSpec(inSpecs[0], outSpec) };
		return outSpecs;
	}

	private DataColumnSpec[] generateOutputColSpec(final DataTableSpec spec) throws InvalidSettingsException {

		DataColumnSpec appendSpec = MolPropsLibrary.getColumnSpec(m_descriptorClassName);
		String colName = appendSpec.getName();
		int uniquifier = 1;
		while (spec.containsName(colName)) {
			colName = appendSpec.getName() + " #" + uniquifier++;
		}
		if (uniquifier > 1) {
			DataColumnSpecCreator c = new DataColumnSpecCreator(appendSpec);
			c.setName(colName);
			appendSpec = c.createSpec();
		}
		return new DataColumnSpec[] { appendSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(m_cdkColSelModel.getStringValue());

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell sCell = row.getCell(colIndex);
				DataCell[] newCells = new DataCell[1];
				if (sCell.isMissing()) {
					Arrays.fill(newCells, DataType.getMissingCell());
					return newCells;
				}
				if (!(sCell instanceof CDKValue)) {
					throw new IllegalArgumentException("No CDK cell at " + colIndex + ": " + sCell.getClass().getName());
				}
				IAtomContainer mol = null;
				try {
					mol = CDKNodeUtils.getExplicitClone(((CDKValue) sCell).getAtomContainer());
				} catch (Exception exception) {
					LOGGER.debug("Unable to parse molecule in row \"" + row.getKey() + "\"", exception);
				}

				Object[] params = new Object[0];
				if (m_descriptorClassName
						.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor")) {
					params = new Object[] { new Boolean(false), new Boolean(false) };
				} else if (m_descriptorClassName
						.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.RuleOfFiveDescriptor")) {
					params = new Object[] { new Boolean(false) };
				}
				newCells[0] = MolPropsLibrary.getProperty(row.getKey().toString(), mol, m_descriptorClassName, params);
				return newCells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				try {
					return generateOutputColSpec(data[0].getDataTableSpec());
				} catch (InvalidSettingsException exception) {
					return null;
				}
			}
		};

		return new ExtendedCellFactory[] { cf };
	}

	/** @see NodeModel#reset() */
	@Override
	protected void reset() {

	}

	/**
	 * @see org.knime.core.node.NodeModel#loadInternals(File, ExecutionMonitor)
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

	}

	/** @see NodeModel#saveInternals(File, ExecutionMonitor) */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

	}

	/** @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO) */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_cdkColSelModel.loadSettingsFrom(settings);
	}

	/** @see NodeModel#saveSettingsTo(NodeSettingsWO) */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_cdkColSelModel.saveSettingsTo(settings);
	}

	/** @see NodeModel#validateSettings(NodeSettingsRO) */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_cdkColSelModel.validateSettings(settings);
	}

	/**
	 * Factory method for the settings holder to be used in NodeModel and NodeDialogPane.
	 * 
	 * @return A new settings model.
	 */
	static SettingsModelString createColSelectorSettingsModel() {

		return new SettingsModelString(CFG_CDK_COL, null);
	}

}
