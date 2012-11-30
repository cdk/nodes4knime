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
package org.openscience.cdk.knime.distance3d;

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
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.similarity.DistanceMoment;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model implementation of Distance3d. Node to evaluate the 3D similarity between two specified molecules as
 * well as generate the 12 descriptors used to characterize the 3D structure.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Distance3dNodeModel extends ThreadedColAppenderNodeModel {

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";

	static final String[] colHeaders = new String[] { "Ctd-Mean", "Ctd-Sigma", "Ctd-Skewness", "Cst-Mean", "Cst-Sigma",
			"Cst-Skewness", "Fct-Mean", "Fct-Sigma", "Fct-Skewness", "Ftf-Mean", "Ftf-Sigma", "Ftf-Skewness" };

	private String colName;

	/**
	 * Constructor for the node model.
	 */
	protected Distance3dNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(colName);

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public DataCell[] getCells(DataRow row) {

				DataCell cdkCell = row.getCell(colIndex);
				DataCell[] momentCells = new DataCell[12];

				if (cdkCell.isMissing()) {
					Arrays.fill(momentCells, DataType.getMissingCell());
					return momentCells;
				}

				checkIsCdkCell(cdkCell);

				IAtomContainer molecule = ((CDKValue) row.getCell(colIndex)).getAtomContainer();
				if (!ConnectivityChecker.isConnected(molecule))
					molecule = ConnectivityChecker.partitionIntoMolecules(molecule).getAtomContainer(0);
				IAtomContainer moleculeMinusH = AtomContainerManipulator.removeHydrogens(molecule);

				try {
					float[] moments = DistanceMoment.generateMoments(moleculeMinusH);

					int i = 0;
					for (float moment : moments) {
						momentCells[i] = new DoubleCell(moment);
						i++;
					}
				} catch (CDKException exception) {
					Arrays.fill(momentCells, DataType.getMissingCell());
				}

				return momentCells;
			}

			private void checkIsCdkCell(DataCell dataCell) {

				if (!(dataCell instanceof CDKValue)) {
					throw new IllegalArgumentException("No CDK cell at " + dataCell + ": "
							+ dataCell.getClass().getName());
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return createOutputTableSpecification();
			}
		};

		return new ExtendedCellFactory[] { cf };
	}

	/**
	 * Creates the table output specification.
	 */
	private DataColumnSpec[] createOutputTableSpecification() {

		DataColumnSpec[] dataColumnSpecs = new DataColumnSpec[12];

		int i = 0;
		for (String header : colHeaders) {
			createColumnSpec(dataColumnSpecs, header, i);
			i++;
		}

		return dataColumnSpecs;
	}

	/**
	 * Creates a single column specification.
	 */
	private void createColumnSpec(DataColumnSpec[] dataColumnSpecs, String colName, int i) {

		DataColumnSpec colSpec = new DataColumnSpecCreator(colName, DoubleCell.TYPE).createSpec();
		dataColumnSpecs[i] = colSpec;
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

		int molColIndex = inSpecs[0].findColumnIndex(colName);
		if (molColIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
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

		if (!inSpecs[0].getColumnSpec(molColIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + colName + "' does not contain CDK cells");
		}

		DataTableSpec outSpec = new DataTableSpec(createOutputTableSpecification());
		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], outSpec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString(CFG_COLNAME, colName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		colName = settings.getString(CFG_COLNAME);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String column = settings.getString(CFG_COLNAME);
		if ((column == null) || (column.length() == 0)) {
			throw new InvalidSettingsException("No CDK molecule column chosen");
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
