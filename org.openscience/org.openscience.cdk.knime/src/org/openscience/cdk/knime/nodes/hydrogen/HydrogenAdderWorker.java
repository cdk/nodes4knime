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
package org.openscience.cdk.knime.nodes.hydrogen;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.SmartAtomContainerManipulator;

/**
 * Multi threaded worker implementation for the Hydrogen Adder Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class HydrogenAdderWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final HydrogenAdderSettings settings;

	public HydrogenAdderWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, final HydrogenAdderSettings settings) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.settings = settings;
		this.columnIndex = columnIndex;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else {
			try {
				CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
				IAtomContainer mol = cdkCell.getAtomContainer();

				if (settings.excludeStereo()) {
					mol = SmartAtomContainerManipulator.suppressNonChiralHydrogens(mol);
				} else {
					mol = AtomContainerManipulator.suppressHydrogens(mol);
				}

				CDKNodeUtils.getFullMolecule(mol);
				outCell = CDKCell.createCDKCell(mol);
			} catch (Throwable t) {
				outCell = DataType.getMissingCell();
			}
		}

		if (settings.replaceColumn()) {
			row = new ReplacedColumnsDataRow(row, outCell, columnIndex);
		} else {
			row = new AppendedColumnRow(row, outCell);
		}
		
		return row;
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
