/*
 * Copyright (c) 2014, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.smarts;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.knime.chem.types.SmartsCell;
import org.knime.chem.types.SmartsValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell3;

public class SmartsNodeModel extends CDKAdapterNodeModel {

	private String colSmarts = "";
	private String colMolecule = "";
	private boolean count = false;

	private int smartsIndex = 0;

	/**
	 * Creates a new model for SMARTS queries.
	 */
	public SmartsNodeModel() {
		super(2, 2, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		List<String> smarts = new ArrayList<String>();
		for (DataRow row : convertedTables[1]) {
			if (!row.getCell(smartsIndex).isMissing()) {
				String smart = ((SmartsCell) row.getCell(smartsIndex)).getSmartsValue();
				smarts.add(smart);
			}
		}

		BufferedDataContainer outputTable[] = new BufferedDataContainer[] {
				exec.createDataContainer(count 
						? appendSpecCount(convertedTables[0].getDataTableSpec()) 
						: appendSpec(convertedTables[0].getDataTableSpec())),
				exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec())) };

		SmartsWorker worker = new SmartsWorker(maxQueueSize, maxParallelWorkers, columnIndex,
				convertedTables[0].getRowCount(), smarts, count, exec, outputTable);

		try {
			worker.run(convertedTables[0]);
		} catch (InterruptedException e) {
			CanceledExecutionException cee = new CanceledExecutionException(e.getMessage());
			cee.initCause(e);
			throw cee;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause == null) {
				cause = e;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		} finally {
			outputTable[0].close();
			outputTable[1].close();
		}

		return new BufferedDataTable[] { outputTable[0].getTable(), outputTable[1].getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		colMolecule = CDKNodeUtils.autoConfigure(inSpecs[0], colMolecule);
		colSmarts = CDKNodeUtils.autoConfigure(inSpecs[1], colSmarts, SmartsValue.class);

		columnIndex = inSpecs[0].findColumnIndex(colMolecule);
		smartsIndex = inSpecs[1].findColumnIndex(colSmarts);

		DataTableSpec outSpec = convertTables(new DataTableSpec[] { inSpecs[0] })[0];
		DataTableSpec outSpecSecond = appendSpec(outSpec);
		DataTableSpec outSpecFirst = outSpecSecond;
		if (count) {
			outSpecFirst = appendSpecCount(outSpec);
		}

		return new DataTableSpec[] { outSpecFirst, outSpecSecond };
	}

	private DataTableSpec appendSpec(DataTableSpec spec) {

		DataColumnSpec[] dcs = new DataColumnSpec[spec.getNumColumns()];
		int i = 0;
		for (DataColumnSpec s : spec) {
			if (i == columnIndex) {
				String name = spec.getColumnNames()[columnIndex];
				dcs[i] = new DataColumnSpecCreator(name, CDKCell3.TYPE).createSpec();
			} else {
				dcs[i] = s;
			}
			i++;
		}
		return new DataTableSpec(dcs);
	}
	
	private DataTableSpec appendSpecCount(DataTableSpec spec) {

		DataColumnSpec[] dcs = new DataColumnSpec[spec.getNumColumns() + 1];
		int i = 0;
		for (DataColumnSpec s : spec) {
			if (i == columnIndex) {
				String name = spec.getColumnNames()[columnIndex];
				dcs[i] = new DataColumnSpecCreator(name, CDKCell3.TYPE).createSpec();
			} else {
				dcs[i] = s;
			}
			i++;
		}
		dcs[i] = new DataColumnSpecCreator("Unique Count", ListCell.getCollectionType(IntCell.TYPE)).createSpec();
		return new DataTableSpec(dcs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		colMolecule = settings.getString("Molecule");
		colSmarts = settings.getString("SMARTS");
		count = settings.getBoolean("Count Unique");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString("Molecule", colMolecule);
		settings.addString("SMARTS", colSmarts);
		settings.addBoolean("Count Unique", count);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String colName = settings.getString("Molecule");
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No column choosen");
		}

		colName = settings.getString("SMARTS");
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No column choosen");
		}
	}
}
