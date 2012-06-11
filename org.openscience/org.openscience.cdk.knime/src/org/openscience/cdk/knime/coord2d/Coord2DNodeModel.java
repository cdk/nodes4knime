/*
 * Created on Nov 29, 2006 12:22:07 PM by thor ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
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

	private static DataColumnSpec createSpec(final DataColumnSpec oldSpec) {

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
						hasCoords = (GeometryTools.has2DCoordinatesNew(m) == 2);
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
										IAtomContainer cp = (IAtomContainer) m.clone();
										new StructureDiagramGenerator(cp).generateCoordinates();
										pClone.set(cp);
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
