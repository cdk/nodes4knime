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
package org.openscience.cdk.knime.masscalculator;

import java.io.File;
import java.io.IOException;

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
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of MassCalculator. This node calculates the molecular weight or molar mass of a sum
 * formula.
 * 
 * @author Stephan Beisken
 */
public class MassCalculatorNodeModel extends ThreadedColAppenderNodeModel {

	// settings
	protected enum Setting {
		COLUMN_NAME("Sum formula"), MASS("Mass");

		private String name;

		Setting(String name) {
			this.name = name;
		}

		public String label() {
			return name;
		}
	};

	private String columnName = "";
	private Mass massOption = Mass.MOLECULAR_WEIGHT;

	private enum Mass {
		MOLAR_WEIGHT, MOLECULAR_WEIGHT
	};

	/**
	 * Constructor for the node model.
	 */
	protected MassCalculatorNodeModel() {
		super(1, 1);
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		columnName = CDKNodeUtils.getColumn(inSpecs[0], columnName, StringValue.class);
		DataColumnSpec columnSpec = new DataColumnSpecCreator(massOption.name().toLowerCase(), DoubleCell.TYPE)
				.createSpec();

		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], new DataTableSpec(columnSpec)) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString(Setting.COLUMN_NAME.label(), columnName);
		settings.addInt(Setting.MASS.label(), massOption == Mass.MOLECULAR_WEIGHT ? 0 : 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		columnName = settings.getString(Setting.COLUMN_NAME.label());
		massOption = settings.getInt(Setting.MASS.label()) == 0 ? Mass.MOLECULAR_WEIGHT : Mass.MOLAR_WEIGHT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		
		String colName = settings.getString(Setting.COLUMN_NAME.label());
		if (colName == null || colName.length() == 0)
			throw new InvalidSettingsException("No sum formula column chosen.");
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

	@Override
	protected ExtendedCellFactory[] prepareExecute(DataTable[] data) throws Exception {

		final int sumFormulaIndex = data[0].getDataTableSpec().findColumnIndex(columnName);
		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell stringCell = row.getCell(sumFormulaIndex);
				if (stringCell.isMissing())
					return new DataCell[] { DataType.getMissingCell() };

				try {
					String sumFormulaString = (String) ((StringValue) stringCell).getStringValue();
					IMolecularFormula sumFormula = MolecularFormulaManipulator.getMolecularFormula(sumFormulaString,
							SilentChemObjectBuilder.getInstance());
					double mass;
					if (massOption == Mass.MOLECULAR_WEIGHT)
						mass = MolecularFormulaManipulator.getMajorIsotopeMass(sumFormula);
					else
						mass = MolecularFormulaManipulator.getNaturalExactMass(sumFormula);

					return new DataCell[] { new DoubleCell(mass) };
				} catch (Throwable t) {
					return new DataCell[] { DataType.getMissingCell() };
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {
				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {
				return new DataColumnSpec[] { new DataColumnSpecCreator(massOption.name().toLowerCase(),
						DoubleCell.TYPE).createSpec() };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
}
