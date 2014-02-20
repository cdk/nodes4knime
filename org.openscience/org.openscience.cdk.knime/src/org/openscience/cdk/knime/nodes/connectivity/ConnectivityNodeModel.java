/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.nodes.connectivity;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.SetCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKAdapterCell;

/**
 * This is the model for the connectivity node that performs all computation by using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeModel extends CDKAdapterNodeModel {

	/**
	 * Creates a new model with one input and one output port.
	 */
	public ConnectivityNodeModel() {
		super(1, 1, new ConnectivitySettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTable = exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec()));

		ConnectivityWorker worker = new ConnectivityWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec,
				outputTable, settings(ConnectivitySettings.class));

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
			outputTable.close();
		}

		return new BufferedDataTable[] { outputTable.getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];

		return new DataTableSpec[] { appendSpec(outSpec) };
	}

	private DataTableSpec appendSpec(DataTableSpec outSpec) {

		DataColumnSpec cs;
		if (settings(ConnectivitySettings.class).addFragmentColumn()) {
			String name = DataTableSpec.getUniqueColumnName(outSpec, "Fragments");
			cs = new DataColumnSpecCreator(name, SetCell.getCollectionType(CDKAdapterCell.RAW_TYPE)).createSpec();
			return new DataTableSpec(outSpec, new DataTableSpec(cs));
		} else {
			DataColumnSpec[] dcs = new DataColumnSpec[outSpec.getNumColumns()];
			int i = 0;
			for (DataColumnSpec spec : outSpec) {
				if (i == columnIndex) {
					String name = outSpec.getColumnNames()[columnIndex];
					dcs[i] = new DataColumnSpecCreator(name, CDKAdapterCell.RAW_TYPE).createSpec();
				} else {
					dcs[i] = spec;
				}
				i++;
			}
			return new DataTableSpec(dcs);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		ConnectivitySettings s = new ConnectivitySettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No compatible molecule column chosen");
		}
	}
}
