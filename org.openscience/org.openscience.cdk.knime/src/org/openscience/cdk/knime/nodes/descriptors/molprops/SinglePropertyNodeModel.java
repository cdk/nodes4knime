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
package org.openscience.cdk.knime.nodes.descriptors.molprops;

import java.util.Arrays;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SinglePropertyNodeModel extends CDKNodeModel {

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

		super(1, 1, null);
		m_cdkColSelModel = createColSelectorSettingsModel();
		m_descriptorClassName = descriptorClassName;
	}

	private DataColumnSpec[] generateOutputColSpec(final DataTableSpec spec) throws InvalidSettingsException {

		DataColumnSpec appendSpec = MolPropsLibrary.getColumnSpec(m_descriptorClassName);
		String colName = DataTableSpec.getUniqueColumnName(spec, appendSpec.getName());
		DataColumnSpecCreator c = new DataColumnSpecCreator(appendSpec);
		c.setName(colName);
		appendSpec = c.createSpec();

		return new DataColumnSpec[] { appendSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		columnIndex = spec.findColumnIndex(m_cdkColSelModel.getStringValue());

		AbstractCellFactory cf = new AbstractCellFactory(true, generateOutputColSpec(spec)) {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell[] newCells = new DataCell[1];

				if (row.getCell(columnIndex).isMissing()
						|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
					Arrays.fill(newCells, DataType.getMissingCell());
					return newCells;
				}

				CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
				IAtomContainer mol = cdkCell.getAtomContainer();

				try {
					mol = CDKNodeUtils.getExplicitClone(mol);
				} catch (Exception exception) {
					LOGGER.debug("Unable to parse molecule in row \"" + row.getKey() + "\"", exception);
				}

				Object[] params = new Object[0];
				if (m_descriptorClassName
						.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.SmartXLogPDescriptor")) {
					params = new Object[] { new Boolean(false) };
				}
				newCells[0] = MolPropsLibrary.getProperty(row.getKey().toString(), mol, m_descriptorClassName, params);
				return newCells;
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.ensureColumnIsConverted(CDKTypeConverter.createConverter(spec, columnIndex), columnIndex);
		arranger.append(cf);
		return arranger;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		m_cdkColSelModel.setStringValue(CDKNodeUtils.autoConfigure(inSpecs, m_cdkColSelModel.getStringValue()));

		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
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
