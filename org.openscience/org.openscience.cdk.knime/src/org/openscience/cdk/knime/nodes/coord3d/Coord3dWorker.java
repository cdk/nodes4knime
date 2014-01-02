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
package org.openscience.cdk.knime.nodes.coord3d;

import java.util.Iterator;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.MultiThreadWorker;
import org.knime.core.util.Pointer;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Multi threaded worker implementation for the Coord3d Worker Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Coord3dWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(Coord3dWorker.class);

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final int timeout;
	private final ExecutorService executor;

	public Coord3dWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, final int timeout) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.timeout = timeout;
		this.columnIndex = columnIndex;

		executor = Executors.newCachedThreadPool();
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else {

			CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
			final IAtomContainer m = cdkCell.getAtomContainer();

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
									fm = ModelBuilder3D.getInstance(SilentChemObjectBuilder.getInstance()).generate3DCoordinates(fm, false);
									col.add(fm);
									pClone.set(col);
								}
							} else {
								IAtomContainer mc = m;
								AtomContainerManipulator.convertImplicitToExplicitHydrogens(mc);
								mc = ModelBuilder3D.getInstance(SilentChemObjectBuilder.getInstance()).generate3DCoordinates(mc, false);
								pClone.set(mc);
							}
						} catch (ThreadDeath d) {
							LOGGER.debug("3D coord generation timed out.");
							throw d;
						} catch (Throwable t) {
							LOGGER.error(t.getMessage(), t);
						}
					}
				};
				Future<?> future = executor.submit(r);
				future.get(timeout, TimeUnit.MILLISECONDS);
				if (pClone.get() != null) {
					outCell = CDKCell.createCDKCell(pClone.get());
				} else {
					outCell = DataType.getMissingCell();
				}
			} catch (Exception ex) {
				if (ex.getMessage() == null) {
					LOGGER.error(row.getKey() + " : " + ex.getClass().getName(), ex);
				} else {
					LOGGER.error(row.getKey() + " : " + ex.getMessage(), ex);
				}
				outCell = DataType.getMissingCell();
			}
		}

		return new ReplacedColumnsDataRow(row, outCell, columnIndex);
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		DataRow append = task.get();
		if (!append.getCell(columnIndex).isMissing()) {
			bdc.addRowToTable(append);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
