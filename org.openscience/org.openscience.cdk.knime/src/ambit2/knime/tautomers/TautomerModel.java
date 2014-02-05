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
package ambit2.knime.tautomers;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell;

import ambit2.knime.tautomers.TautomerWorker.Mode;

/**
 * Model implementation for AMBIT's tautomer generator node.
 * 
 * @author Stephan Beisken, EMBL-EBI
 */
public class TautomerModel extends CDKAdapterNodeModel {

	/**
	 * Constructor for the node model.
	 */
	public TautomerModel() {
		super(1, 1, new TautomerSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		// create the output data container
		BufferedDataContainer bdc = exec.createDataContainer(outSpec(convertedTables[0].getDataTableSpec()));

		// create the tautomer worker
		int rowCount = convertedTables[0].getRowCount();
		TautomerWorker worker = new TautomerWorker(maxQueueSize, maxParallelWorkers, rowCount, exec, bdc, columnIndex);
		// set execution mode
		worker.executionMode(settings(TautomerSettings.class).mode());

		try { // run, cascade exceptions
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
			bdc.close();
		}

		// return the buffered data table from the container
		return new BufferedDataTable[] { bdc.getTable() };
	}

	/**
	 * Returns the output data table specification.
	 * 
	 * @param inSpec the input data table specification
	 * @return the output table specification
	 */
	private DataTableSpec outSpec(DataTableSpec inSpec) {

		DataTableSpec outSpec = inSpec;

		// if the CDK cell is not to be replaced, append a CDK and Double cell
		if (settings(TautomerSettings.class).mode() != Mode.BEST_REPLACE) {
			String tautomerName = DataTableSpec.getUniqueColumnName(inSpec, "Tautomer");
			DataColumnSpec tautomerSpec = new DataColumnSpecCreator(tautomerName, CDKCell.TYPE).createSpec();
			String score = DataTableSpec.getUniqueColumnName(inSpec, "Score");
			DataColumnSpec rankSpec = new DataColumnSpecCreator(score, DoubleCell.TYPE).createSpec();
			outSpec = new DataTableSpec(inSpec, new DataTableSpec(tautomerSpec, rankSpec));
		}

		return outSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec convertedSpec = convertTables(inSpecs)[0];

		return new DataTableSpec[] { outSpec(convertedSpec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {

		TautomerSettings s = new TautomerSettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
	}
}
