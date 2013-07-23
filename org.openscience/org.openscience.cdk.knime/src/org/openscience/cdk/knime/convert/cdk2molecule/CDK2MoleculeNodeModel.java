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
package org.openscience.cdk.knime.convert.cdk2molecule;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.chem.types.CMLCell;
import org.knime.chem.types.Mol2Cell;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.convert.cdk2molecule.CDK2MoleculeSettings.Format;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This class is the model for the CDK->Molecule node. It converts CDK molecules into different textual representations.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDK2MoleculeNodeModel extends ThreadedColAppenderNodeModel {

	private final CDK2MoleculeSettings m_settings = new CDK2MoleculeSettings();

	/**
	 * Creates a new model.
	 */
	public CDK2MoleculeNodeModel() {

		super(1, 1);

		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int molColIndex = inSpecs[0].findColumnIndex(m_settings.columnName());
		if (molColIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
				if (spec.getType().isCompatible(CDKValue.class)) {
					setWarningMessage("Column '" + spec.getName() + "' automatically chosen as molecule column");
					molColIndex = i;
					break;
				}
				i++;
			}

			if (molColIndex == -1) {
				throw new InvalidSettingsException("Column '" + m_settings.columnName() + "' does not exist");
			}
		}

		if (!inSpecs[0].getColumnSpec(molColIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + m_settings.columnName() + "' does not contain CDK cells");
		}

		DataType type = null;
		if (m_settings.destFormat() == Format.SDF) {
			type = SdfCell.TYPE;
		} else if (m_settings.destFormat() == Format.Smiles) {
			type = SmilesCell.TYPE;
		} else if (m_settings.destFormat() == Format.Mol2) {
			type = Mol2Cell.TYPE;
		} else if (m_settings.destFormat() == Format.CML) {
			type = CMLCell.TYPE;
		}

		DataTableSpec outSpec;
		if (m_settings.replaceColumn()) {
			DataColumnSpecCreator crea = new DataColumnSpecCreator(m_settings.columnName(), type);
			outSpec = ReplacedColumnsTable.createTableSpec(inSpecs[0], crea.createSpec(), molColIndex);
		} else {
			DataColumnSpecCreator crea = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpecs[0],
					m_settings.newColumnName()), type);
			outSpec = AppendedColumnTable.getTableSpec(inSpecs[0], crea.createSpec());
		}

		return new DataTableSpec[] { outSpec };
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

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

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

		CDK2MoleculeSettings s = new CDK2MoleculeSettings();
		s.loadSettings(settings);

		if (s.columnName() == null) {
			throw new InvalidSettingsException("No column selected");
		}

		if (!s.replaceColumn() && s.newColumnName().length() < 1) {
			throw new InvalidSettingsException("No name for the new column entered");
		}
	}
}
