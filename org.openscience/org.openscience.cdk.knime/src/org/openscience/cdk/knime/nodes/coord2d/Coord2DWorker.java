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
package org.openscience.cdk.knime.nodes.coord2d;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Multi threaded worker implementation for the Coord2d Worker Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Coord2DWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final static NodeLogger LOGGER = NodeLogger.getLogger(Coord2DWorker.class);

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final boolean force;

	public Coord2DWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, final boolean force) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.force = force;
		this.columnIndex = columnIndex;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else {
			CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
			IAtomContainer m = cdkCell.getAtomContainer();

			try {
				m = CDKNodeUtils.calculateCoordinates(m, force, false);
			} catch (ThreadDeath d) {
				LOGGER.debug("2D coord generation" + " timed out for row \"" + row.getKey() + "\"");
				throw d;
			} catch (Throwable t) {
				LOGGER.error(t.getMessage(), t);
			}

			outCell = CDKCell.createCDKCell(m);
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
