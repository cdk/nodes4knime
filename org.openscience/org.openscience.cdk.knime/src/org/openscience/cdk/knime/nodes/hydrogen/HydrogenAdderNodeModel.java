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
package org.openscience.cdk.knime.nodes.hydrogen;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This is the model for the hydrogen node that performs all computation by using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class HydrogenAdderNodeModel extends CDKAdapterNodeModel {

	/**
	 * Creates a new model having one input and one output node.
	 */
	public HydrogenAdderNodeModel() {
		super(1, 1, new HydrogenAdderSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		HydrogenAdderSettings s = new HydrogenAdderSettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTable = exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec()));

		HydrogenAdderWorker worker = new HydrogenAdderWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec,
				outputTable, settings(HydrogenAdderSettings.class));

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

	private DataTableSpec appendSpec(DataTableSpec spec) {

		DataColumnSpec cs;
		if (settings(HydrogenAdderSettings.class).replaceColumn()) {
			DataColumnSpec[] dcs = new DataColumnSpec[spec.getNumColumns()];
			int i = 0;
			for (DataColumnSpec s : spec) {
				if (i == columnIndex) {
					String name = spec.getColumnNames()[columnIndex];
					dcs[i] = new DataColumnSpecCreator(name, CDKCell.TYPE).createSpec();
				} else {
					dcs[i] = s;
				}
				i++;
			}
			return new DataTableSpec(dcs);
		} else {
			String name = DataTableSpec.getUniqueColumnName(spec, settings(HydrogenAdderSettings.class)
					.appendColumnName());
			cs = new DataColumnSpecCreator(name, CDKCell.TYPE).createSpec();
			return new DataTableSpec(spec, new DataTableSpec(cs));
		}
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
}
