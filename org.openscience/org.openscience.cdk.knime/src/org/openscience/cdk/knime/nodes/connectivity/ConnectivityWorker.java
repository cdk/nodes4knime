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
package org.openscience.cdk.knime.nodes.connectivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKCell2;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Multi threaded worker implementation for the Connectivity Worker Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ConnectivityWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final ConnectivitySettings settings;

	public ConnectivityWorker(int maxQueueSize, int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, ConnectivitySettings settings) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.columnIndex = columnIndex;
		this.bdc = bdc;
		this.settings = settings;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else if (settings.removeCompleteRow()) {
			outCell = removeRows(row);
		} else if (settings.addFragmentColumn()) {
			outCell = addFragments(row);
		} else {
			outCell = retainBiggest(row);
		}

		if (settings.addFragmentColumn()) {
			return new AppendedColumnRow(row, outCell);
		} else {
			return new ReplacedColumnsDataRow(row, outCell, columnIndex);
		}
	}

	private DataCell removeRows(final DataRow inRow) {

		CDKValue cdkCell = ((AdapterValue) inRow.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer mol = cdkCell.getAtomContainer();
		if (ConnectivityChecker.isConnected(mol)) {
			return CDKCell2.createCDKCell(mol);
		} else {
			return DataType.getMissingCell();
		}
	}

	private DataCell retainBiggest(final DataRow inRow) {

		CDKValue cdkCell = ((AdapterValue) inRow.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer mol = cdkCell.getAtomContainer();

		if (!ConnectivityChecker.isConnected(mol)) {
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			IAtomContainer biggest = molSet.getAtomContainer(0);
			for (int i = 1; i < molSet.getAtomContainerCount(); i++) {
				if (molSet.getAtomContainer(i).getBondCount() > biggest.getBondCount()) {
					biggest = molSet.getAtomContainer(i);
				}
			}
			return CDKCell2.createCDKCell(biggest);
		} else {
			return CDKCell2.createCDKCell(mol);
		}
	}

	private DataCell addFragments(final DataRow inRow) {

		CDKValue cdkCell = ((AdapterValue) inRow.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer mol = cdkCell.getAtomContainer();

		if (!ConnectivityChecker.isConnected(mol)) {
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			List<DataCell> cells = new ArrayList<DataCell>(molSet.getAtomContainerCount());

			IAtomContainer singleMol;
			for (int i = 0; i < molSet.getAtomContainerCount(); i++) {
				singleMol = molSet.getAtomContainer(i);
				cells.add(CDKCell2.createCDKCell(singleMol));
			}
			return CollectionCellFactory.createSetCell(cells);
		} else {
			return CollectionCellFactory.createSetCell(Collections.singleton(CDKCell2.createCDKCell(mol)));
		}
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
