/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.distance3d.similarity;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.RowAppender;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.similarity.DistanceMoment;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model implementation of DistanceSimilarity. Node to evaluate the 3D similarity between two specified
 * molecules.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class DistanceSimilarityNodeModel extends ThreadedTableBuilderNodeModel {

	/** Config key for column name. */
	static final String QUE_COLNAME = "queName";
	static final String TAR_COLNAME = "tarName";

	private String queName;
	private int molColIndex;
	private String tarName;
	private int tarColIndex;

	private IAtomContainer targetMinusH;

	/**
	 * Constructor for the node model.
	 */
	protected DistanceSimilarityNodeModel() {

		super(2, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		tarColIndex = data[1].getDataTableSpec().findColumnIndex(tarName);
		IAtomContainer target = null;
		for (DataRow row : data[1]) {
			target = ((CDKValue) row.getCell(tarColIndex)).getAtomContainer();
			break;
		}
		targetMinusH = AtomContainerManipulator.removeHydrogens(target);
		molColIndex = data[0].getDataTableSpec().findColumnIndex(queName);

		return getDataTableSpec(data[0].getDataTableSpec());
	}

	private DataTableSpec[] getDataTableSpec(DataTableSpec spec) {

		String newColName = "Distance Similarity";
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);

		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DoubleCell.TYPE);
		DataTableSpec appendSpec = new DataTableSpec(c.createSpec());

		return new DataTableSpec[] { new DataTableSpec(spec, appendSpec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		if (inRow.getCell(molColIndex).isMissing()) {
			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, DataType.getMissingCell()));
			return;
		}

		double sim = -1;
		try {
			IAtomContainer queryMinusH = AtomContainerManipulator.removeHydrogens(((CDKValue) inRow
					.getCell(molColIndex)).getAtomContainer());
			sim = DistanceMoment.calculate(queryMinusH, targetMinusH);
		} catch (CDKException exception) {
			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, DataType.getMissingCell()));
			return;
		}

		outputTables[0].addRowToTable(new AppendedColumnRow(inRow, new DoubleCell(sim)));
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

		configureBySpec(QUE_COLNAME, inSpecs[0]);
		configureBySpec(TAR_COLNAME, inSpecs[1]);

		return getDataTableSpec(inSpecs[0]);
	}

	private void configureBySpec(String colName, DataTableSpec inSpec) throws InvalidSettingsException {

		int molColIndex = inSpec.findColumnIndex(colName);
		if (molColIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpec) {
				if (spec.getType().isCompatible(CDKValue.class)) {
					if (molColIndex != -1) {
						setWarningMessage("Column '" + spec.getName() + "' automatically chosen as molecule column");
					}
					molColIndex = i;
					colName = spec.getName();
				}
				i++;
			}

			if (molColIndex == -1) {
				throw new InvalidSettingsException("Column '" + colName + "' does not exist");
			}
		}

		if (!inSpec.getColumnSpec(molColIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + colName + "' does not contain CDK cells");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (queName != null) {
			settings.addString(QUE_COLNAME, queName);
			settings.addString(TAR_COLNAME, tarName);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		queName = settings.getString(QUE_COLNAME);
		tarName = settings.getString(TAR_COLNAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String colName = settings.getString(QUE_COLNAME);
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No query column choosen");
		}

		colName = settings.getString(TAR_COLNAME);
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No target column choosen");
		}
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

}
