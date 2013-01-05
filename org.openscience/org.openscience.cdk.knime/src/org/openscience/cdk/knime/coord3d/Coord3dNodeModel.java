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
package org.openscience.cdk.knime.coord3d;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pointer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.coord2d.Coord2DNodeModel;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model implementation of Coord3d. Integrates the CDK 3D Model Builder to calculate 3D coordinates for CDK
 * molecules.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Coord3dNodeModel extends ThreadedColAppenderNodeModel {

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";
	static final String TIMEOUT = "timeout";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(Coord2DNodeModel.class);

	private String m_colName;
	private int timeout = 10000;

	/**
	 * Creates a new model for 3D coordinate generation.
	 */
	public Coord3dNodeModel() {

		super(1, 1);
		
		this.setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int molColIndex = inSpecs[0].findColumnIndex(m_colName);
		if (molColIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
				if (spec.getType().isCompatible(CDKValue.class)) {
					if (molColIndex != -1) {
						setWarningMessage("Column '" + spec.getName() + "' automatically chosen as molecule column");
					}
					molColIndex = i;
					m_colName = spec.getName();
				}
				i++;
			}

			if (molColIndex == -1) {
				throw new InvalidSettingsException("Column '" + m_colName + "' does not exist");
			}
		}

		if (!inSpecs[0].getColumnSpec(molColIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + m_colName + "' does not contain CDK cells");
		}

		return new DataTableSpec[] { inSpecs[0] };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_colName = settings.getString(CFG_COLNAME);
		timeout = settings.getInt(TIMEOUT);
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

		if (m_colName != null) {
			settings.addString(CFG_COLNAME, m_colName);
			settings.addInt(TIMEOUT, timeout);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String colName = settings.getString(CFG_COLNAME);
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No column choosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(m_colName);
		final ExecutorService executor = Executors.newCachedThreadPool();

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell[] cells = new DataCell[1];

				if (row.getCell(colIndex).isMissing()) {
					cells[0] = DataType.getMissingCell();
					return cells;
				}

				DataCell oldCell = row.getCell(colIndex);
				final IAtomContainer m = ((CDKValue) oldCell).getAtomContainer();

				try {
					final Pointer<IAtomContainer> pClone = new Pointer<IAtomContainer>();
					Runnable r = new Runnable() {

						@Override
						public void run() {

							try {
								if (!ConnectivityChecker.isConnected(m)) {
									IAtomContainerSet mSet = ConnectivityChecker.partitionIntoMolecules(m);
									Iterator<IAtomContainer> it = mSet.atomContainers().iterator();
									IAtomContainer col = new AtomContainer();
									while (it.hasNext()) {
										IAtomContainer fm = (IAtomContainer) it.next();
										AtomContainerManipulator.convertImplicitToExplicitHydrogens(fm);
										fm = ModelBuilder3D.getInstance().generate3DCoordinates(fm, false);
										col.add(fm);
										pClone.set(col);
									}
								} else {
									IAtomContainer mc = m;
									AtomContainerManipulator.convertImplicitToExplicitHydrogens(mc);
									mc = ModelBuilder3D.getInstance().generate3DCoordinates(mc, false);
									pClone.set(mc);
								}
							} catch (ThreadDeath d) {
								LOGGER.debug("3D coord generation timed out for row \"" + row.getKey() + "\"");
								throw d;
							} catch (Throwable t) {
								LOGGER.error(t.getMessage(), t);
							}
						}
					};
					Future<?> future = executor.submit(r);
					future.get(timeout, TimeUnit.MILLISECONDS);
					if (pClone.get() != null) {
						cells[0] = new CDKCell(pClone.get());
					} else {
						cells[0] = DataType.getMissingCell();
					}
				} catch (Exception ex) {
					if (ex.getMessage() == null) {
						LOGGER.error(row.getKey() + " : " + ex.getClass().getName(), ex);
					} else {
						LOGGER.error(row.getKey() + " : " + ex.getMessage(), ex);
					}
					cells[0] = DataType.getMissingCell();
				}

				return cells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new ReplaceColumn(colIndex) };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return new DataColumnSpec[] { data[0].getDataTableSpec().getColumnSpec(colIndex) };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
}
