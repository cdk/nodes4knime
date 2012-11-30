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
package org.openscience.cdk.knime.coord2d;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.layout.StructureDiagramGenerator;

/**
 * This class is the model for the CDK 2D-generation node. It takes the input molecules (if there are any), checks if
 * they already have 2D coordinates assigned and if not creates a new copy of the molecule and computes 2D coordinates
 * for it. The columns with molecules will have a property afterwards, that indicates that 3D coordinates are available
 * ( {@link CDKCell#COORD2D_AVAILABLE}).
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class Coord2DNodeModel extends ThreadedColAppenderNodeModel {

	private static final Map<String, String> PROP_2D;

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";

	static final String FORCE = "force";
	static final int TIMEOUT = 5000;

	private static final NodeLogger LOGGER = NodeLogger.getLogger(Coord2DNodeModel.class);

	static {
		Map<String, String> temp = new TreeMap<String, String>();
		temp.put(CDKCell.COORD2D_AVAILABLE, "true");
		PROP_2D = Collections.unmodifiableMap(temp);
	}

	private String m_colName;

	private boolean m_force;

	/**
	 * Creates a new model for 2D coordinate generation.
	 */
	public Coord2DNodeModel() {

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

		DataColumnSpec[] columns = new DataColumnSpec[inSpecs[0].getNumColumns()];
		int i = 0;
		for (DataColumnSpec spec : inSpecs[0]) {
			if (i == molColIndex) {
				columns[i] = createSpec(spec);
			} else {
				columns[i] = spec;
			}
			i++;
		}

		return new DataTableSpec[] { new DataTableSpec(columns) };
	}

	private DataColumnSpec createSpec(final DataColumnSpec oldSpec) {

		DataColumnSpecCreator c = new DataColumnSpecCreator(oldSpec);
		DataColumnProperties props = oldSpec.getProperties().cloneAndOverwrite(PROP_2D);
		c.setProperties(props);
		return c.createSpec();
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
		m_force = settings.getBoolean(FORCE, false);
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

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (m_colName != null) {
			settings.addString(CFG_COLNAME, m_colName);
		}
		settings.addBoolean(FORCE, m_force);
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
		if (colIndex == -1) {
			throw new InvalidSettingsException("Column '" + m_colName + "' does not exist in input table");
		}
		if (!data[0].getDataTableSpec().getColumnSpec(colIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + m_colName + "' does not contain CDK cells");
		}

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
				boolean hasCoords = false;
				if (!m_force) {
					try { // sometimes this check throws an exception...
						hasCoords = (GeometryTools.has2DCoordinates(m));
					} catch (Exception e) {
						hasCoords = false;
					}
				}
				if (hasCoords) {
					cells[0] = oldCell;
				} else {
					try {
						final Pointer<IAtomContainer> pClone = new Pointer<IAtomContainer>();
						// bug fix: added thread-wrapper/watchdog timer to stop
						// endless runs[...
						Runnable r = new Runnable() {

							@Override
							public void run() {

								try {
									// bug: SDG works for connected molecules only
									// quick fix: superimpose
									if (!ConnectivityChecker.isConnected(m)) {
										IAtomContainerSet mSet = ConnectivityChecker.partitionIntoMolecules(m);
										Iterator<IAtomContainer> it = mSet.atomContainers().iterator();
										IAtomContainer col = new AtomContainer();
										while (it.hasNext()) {
											IAtomContainer fm = it.next();
											new StructureDiagramGenerator(fm).generateCoordinates();
											col.add(fm);
											pClone.set(col);
										}
									} else {
										new StructureDiagramGenerator(m).generateCoordinates();
										pClone.set(m);
									}
								} catch (ThreadDeath d) {
									LOGGER.debug("2D coord generation" + " timed out for row \"" + row.getKey() + "\"");
									throw d;
								} catch (Throwable t) {
									LOGGER.error(t.getMessage(), t);
								}
							}
						};
						Future<?> future = executor.submit(r);
						future.get(TIMEOUT, TimeUnit.MILLISECONDS);
						if (pClone.get() != null) {
							cells[0] = new CDKCell(pClone.get());
						} else {
							cells[0] = oldCell;
						}
					} catch (Exception ex) {
						if (ex.getMessage() == null) {
							LOGGER.error(row.getKey() + " : " + ex.getClass().getName(), ex);
						} else {
							LOGGER.error(row.getKey() + " : " + ex.getMessage(), ex);
						}
						cells[0] = DataType.getMissingCell();
					}
				}

				return cells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new ReplaceColumn(colIndex) };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return new DataColumnSpec[] { createSpec(data[0].getDataTableSpec().getColumnSpec(colIndex)) };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
}
