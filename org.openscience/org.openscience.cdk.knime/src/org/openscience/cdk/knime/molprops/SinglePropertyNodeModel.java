/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SinglePropertyNodeModel extends NodeModel {

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
		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { rearranger.createSpec() };
	}

	private ColumnRearranger createColumnRearranger(final DataTableSpec in) throws InvalidSettingsException {

		String name = m_cdkColSelModel.getStringValue();
		int cdkColIndex = in.findColumnIndex(name);
		if (cdkColIndex < 0) {
			throw new InvalidSettingsException("No such column \"" + name + "\" in input table.");
		}
		DataColumnSpec cdkColSpec = in.getColumnSpec(cdkColIndex);
		if (!cdkColSpec.getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column \"" + name + "\" does not contain CDK molecules");
		}
		DataColumnSpec appendSpec = MolPropsLibrary.getColumnSpec(m_descriptorClassName);
		String colName = appendSpec.getName();
		int uniquifier = 1;
		while (in.containsName(colName)) {
			colName = appendSpec.getName() + " #" + uniquifier++;
		}
		if (uniquifier > 1) {
			DataColumnSpecCreator c = new DataColumnSpecCreator(appendSpec);
			c.setName(colName);
			appendSpec = c.createSpec();
		}
		MolPropsGenerator generator = new MolPropsGenerator(cdkColIndex, new String[] { m_descriptorClassName },
				new DataColumnSpec[] { appendSpec });
		ColumnRearranger rearrange = new ColumnRearranger(in);
		rearrange.append(generator);
		return rearrange;
	}

	/** @see NodeModel#execute(BufferedDataTable[], ExecutionContext) */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger rearranger = createColumnRearranger(inData[0].getDataTableSpec());
		BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
		return new BufferedDataTable[] { out };
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
