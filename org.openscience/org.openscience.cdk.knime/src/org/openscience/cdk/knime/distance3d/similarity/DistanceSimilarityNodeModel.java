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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
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
public class DistanceSimilarityNodeModel extends NodeModel {

	/** Config key for column name. */
	static final String QUE_COLNAME = "queName";
	static final String TAR_COLNAME = "tarName";

	private String queName;
	private String tarName;

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
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger cr = createColumnRearranger(inData[0].getDataTableSpec());

		final int tarColIndex = inData[1].getSpec().findColumnIndex(tarName);
		IAtomContainer target = null;
		for (DataRow row : inData[1]) {
			target = ((CDKValue) row.getCell(tarColIndex)).getAtomContainer();
			break;
		}
		targetMinusH = AtomContainerManipulator.removeHydrogens(target);

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], cr, exec) };
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
		
		ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { rearranger.createSpec() };
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

	private ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		String newColName = "Distance Similarity";
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);

		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DoubleCell.TYPE);
		DataColumnSpec appendSpec = c.createSpec();
		final int molColIndex = spec.findColumnIndex(queName);

		SingleCellFactory cf = new SingleCellFactory(appendSpec) {

			@Override
			public DataCell getCell(final DataRow row) {

				if (row.getCell(molColIndex).isMissing()) {
					return DataType.getMissingCell();
				}

				double sim = -1;
				try {
					CDKValue mol = (CDKValue) row.getCell(molColIndex);
					IAtomContainer queryMinusH = AtomContainerManipulator.removeHydrogens(mol.getAtomContainer());
					sim = DistanceMoment.calculate(queryMinusH, targetMinusH);
				} catch (CDKException exception) {
					return DataType.getMissingCell();
				}

				return new DoubleCell(sim);
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.append(cf);
		return arranger;
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
