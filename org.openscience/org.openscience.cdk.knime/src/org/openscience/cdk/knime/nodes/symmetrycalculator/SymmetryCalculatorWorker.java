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
package org.openscience.cdk.knime.nodes.symmetrycalculator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.invariant.EquivalentClassPartitioner;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;

/**
 * Multi threaded worker implementation for the Symmetry Calculator Worker
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SymmetryCalculatorWorker extends MultiThreadWorker<DataRow, List<DataRow>> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final int addNbColumns;
	private final boolean visual;

	public SymmetryCalculatorWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, final int addNbColumns, final boolean visual) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.addNbColumns = addNbColumns;
		this.columnIndex = columnIndex;
		this.exec = exec;
		this.bdc = bdc;
		this.visual = visual;
	}

	@Override
	protected List<DataRow> compute(DataRow row, long index) throws Exception {
		
		List<DataRow> outRows = new ArrayList<DataRow>();
		
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			return getMissing(row);
		}

		CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer m = cdkCell.getAtomContainer();

		if (m == null || !ConnectivityChecker.isConnected(m)) {
			return getMissing(row);
		} else {
			m = SMSDNormalizer.convertExplicitToImplicitHydrogens(m);

			int atomId = 1;
			Map<String, Integer> parentIdMap = new HashMap<String, Integer>();
			// make a map of the sequential numbering based
			if (CDKNodePlugin.numbering() == NUMBERING.SEQUENTIAL) {
				for (IAtom atom : m.atoms()) {
					parentIdMap.put(atom.getID(), atomId);
					atomId++;
				}
			}

			// partition the atoms into equivalent classes
			int[] partitions = null;
			try {
				partitions = new EquivalentClassPartitioner(m).getTopoEquivClassbyHuXu(m);
			} catch (Exception exception) {
				// do nothing
			}
			int count = 0;

			if (partitions == null || partitions[partitions.length - 1] > partitions[0]) {
				return getMissing(row);
			}

			int[] counts = new int[partitions[0]];
			int parts = 0;
			for (int i = 1; i < partitions.length; i++) {
				counts[partitions[i] - 1]++;
				if (counts[partitions[i] - 1] == 2)
					parts++;
			}

			if (!visual) {
				for (int i = 1; i < partitions.length; i++) {

					DataCell[] outCells = new DataCell[addNbColumns];
					if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
						outCells[0] = new StringCell(m.getAtom(i - 1).getID());
					} else {
						outCells[0] = new StringCell("" + parentIdMap.get(m.getAtom(i - 1).getID()));
					}
					outCells[1] = new IntCell(partitions[i]);
					outRows.add(new AppendedColumnRow(new RowKey(row.getKey().getString() + "_" + count), row, outCells));
					count++;
				}
			} else {
				int k = 0;
				Color[] colors = CDKNodeUtils.generateColors(parts);
				Map<Integer, Integer> colorsUsed = new HashMap<Integer, Integer>();

				for (int i = 1; i < partitions.length; i++) {
					if (counts[partitions[i] - 1] > 1) {
						if (!colorsUsed.containsKey(partitions[i] - 1)) {
							colorsUsed.put(partitions[i] - 1, k);
							k++;
						}
						m.getAtom(i - 1).setProperty(CDKConstants.ANNOTATIONS,
								colors[colorsUsed.get(partitions[i] - 1)].getRGB());
					}
				}
				DataCell[] image = new DataCell[] { CDKCell.createCDKCell(m) };
				outRows.add(new AppendedColumnRow(row, image));
			}
		}

		return outRows;
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		List<DataRow> append = task.get();

		for (DataRow row : append) {
			bdc.addRowToTable(row);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}

	private List<DataRow> getMissing(DataRow row) {

		DataCell[] outCells = new DataCell[addNbColumns];
		Arrays.fill(outCells, DataType.getMissingCell());
		List<DataRow> outRows = new ArrayList<DataRow>();
		outRows.add(new AppendedColumnRow(row, outCells));
		return outRows;
	}
}
