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
package org.openscience.cdk.knime.nodes.masscalculator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of MassCalculator. This node calculates the molecular weight or molar mass of a sum
 * formula.
 * 
 * @author Stephan Beisken
 */
public class MassCalculatorNodeModel extends CDKNodeModel {

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
		super(1, 1, null);
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// auto-configures the settings for an available molecule column if none is set
		columnName = CDKNodeUtils.autoConfigure(inSpecs, columnName, StringValue.class);

		// creates the column rearranger -- does the heavy lifting for adapter cells
		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
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
		if (colName == null || colName.length() == 0) {
			throw new InvalidSettingsException("No sum formula column chosen.");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(DataTableSpec spec) throws InvalidSettingsException {

		final int sumFormulaIndex = spec.findColumnIndex(columnName);

		DataColumnSpec appendSpec = new DataColumnSpecCreator(massOption.name().toLowerCase(), DoubleCell.TYPE)
				.createSpec();

		SingleCellFactory cf = new SingleCellFactory(true, appendSpec) {

			@Override
			public DataCell getCell(final DataRow row) {

				DataCell cell = row.getCell(sumFormulaIndex);
				if (cell.isMissing()) {
					return DataType.getMissingCell();
				}

				try {
					String sumFormulaString = (String) ((StringValue) cell).getStringValue();
					IMolecularFormula sumFormula = MolecularFormulaManipulator.getMolecularFormula(sumFormulaString,
							SilentChemObjectBuilder.getInstance());
					double mass;
					if (massOption == Mass.MOLECULAR_WEIGHT) {
						mass = MolecularFormulaManipulator.getMajorIsotopeMass(sumFormula);
					} else {
						mass = MolecularFormulaManipulator.getNaturalExactMass(sumFormula);
					}

					return new DoubleCell(mass);
				} catch (Throwable t) {
					return DataType.getMissingCell();
				}
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.append(cf);
		return arranger;
	}
}
