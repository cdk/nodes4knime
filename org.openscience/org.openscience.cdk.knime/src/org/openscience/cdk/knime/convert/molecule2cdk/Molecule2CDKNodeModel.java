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
package org.openscience.cdk.knime.convert.molecule2cdk;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This is the model for the Molecule->CDK node that converts molecules' string representations into CDK objects. The
 * conversion is done in parallel because the model optionally also computes 2D coordinates and parsing Smiles may take
 * a long time by itself.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Molecule2CDKNodeModel extends ThreadedColAppenderNodeModel {

	private final Molecule2CDKSettings m_settings = new Molecule2CDKSettings();

	/**
	 * Creates a new model.
	 */
	public Molecule2CDKNodeModel() {

		super(1, 1);
		
		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		return new ExtendedCellFactory[] { new MolConverter(data[0].getDataTableSpec(), m_settings) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int colIndex = inSpecs[0].findColumnIndex(m_settings.columnName());
		if (colIndex == -1) {
			int index = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
				DataType t = spec.getType();
				if (t.isCompatible(SdfValue.class) || t.isCompatible(MolValue.class)
						|| t.isCompatible(SmilesValue.class) || t.isCompatible(Mol2Value.class)) {
					if (colIndex != -1) {
						setWarningMessage("Auto-selected column '" + spec.getName() + "'");
					}
					colIndex = index;
				}
				index++;
			}
			if (colIndex == -1) {
				throw new InvalidSettingsException("No molecule column found");
			}
			m_settings.columnName(inSpecs[0].getColumnSpec(colIndex).getName());
		} else {
			DataType t = inSpecs[0].getColumnSpec(colIndex).getType();
			if (!(t.isCompatible(SdfValue.class) || t.isCompatible(MolValue.class) || t.isCompatible(SmilesValue.class)
					|| t.isCompatible(Mol2Value.class) || t.isCompatible(StringValue.class))) {
				throw new InvalidSettingsException("Column '" + m_settings.columnName()
						+ "' is not a supported molecule column");
			}
		}

		DataTableSpec outSpec;
		if (m_settings.replaceColumn()) {
			DataColumnSpecCreator crea = new DataColumnSpecCreator(m_settings.columnName(), CDKCell.TYPE);
			outSpec = ReplacedColumnsTable.createTableSpec(inSpecs[0], crea.createSpec(), colIndex);
		} else {
			DataColumnSpecCreator crea = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpecs[0],
					m_settings.newColumnName()), CDKCell.TYPE);
			outSpec = AppendedColumnTable.getTableSpec(inSpecs[0], crea.createSpec());
		}

		return new DataTableSpec[] { outSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_settings.loadSettings(settings);
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
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		Molecule2CDKSettings s = new Molecule2CDKSettings();
		s.loadSettings(settings);
		if (!s.replaceColumn() && ((s.newColumnName() == null) || (s.newColumnName().length() < 1))) {
			throw new InvalidSettingsException("No name for new column given");
		}
	}
}
