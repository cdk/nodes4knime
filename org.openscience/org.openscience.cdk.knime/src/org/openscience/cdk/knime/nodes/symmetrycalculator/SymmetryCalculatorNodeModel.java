/*
 * Copyright (c) 2013, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This is the model implementation of SymmetryCalculator.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class SymmetryCalculatorNodeModel extends CDKAdapterNodeModel {

	static final String VISUAL = "visual";
	static final String CFG_COLNAME = "colName";

	private String colName;
	private boolean visual;

	/**
	 * Constructor for the node model.
	 */
	protected SymmetryCalculatorNodeModel() {
		super(1, 1, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		DataTableSpec convertedSpec = convertedTables[0].getDataTableSpec();
		DataTableSpec appendSpec = appendSpec(convertedSpec);
		DataTableSpec outSpec = new DataTableSpec(convertedSpec, appendSpec);

		BufferedDataContainer outputTable = exec.createDataContainer(outSpec);

		int addColumns = (visual) ? 1 : 2;
		SymmetryCalculatorWorker worker = new SymmetryCalculatorWorker(maxQueueSize, maxParallelWorkers, columnIndex,
				exec, outputTable, addColumns, visual);

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

		colName = CDKNodeUtils.autoConfigure(inSpecs[0], colName);
		DataTableSpec outSpec = convertTables(inSpecs)[0];
		DataTableSpec appendSpec = appendSpec(outSpec);

		return new DataTableSpec[] { new DataTableSpec(outSpec, appendSpec) };
	}

	private DataTableSpec appendSpec(final DataTableSpec inSpecs) {

		int addColumns = (visual) ? 1 : 2;

		DataColumnSpec[] cs = new DataColumnSpec[addColumns];

		if (visual) {
			String name = DataTableSpec.getUniqueColumnName(inSpecs, "Equivalent Class Rendering");
			cs[0] = new DataColumnSpecCreator(name, CDKCell.TYPE).createSpec();
		} else {
			String nameCol1 = DataTableSpec.getUniqueColumnName(inSpecs, "Atom ID");
			cs[0] = new DataColumnSpecCreator(nameCol1, StringCell.TYPE).createSpec();
			String nameCol2 = DataTableSpec.getUniqueColumnName(inSpecs, "Equivalent Class");
			cs[1] = new DataColumnSpecCreator(nameCol2, IntCell.TYPE).createSpec();
		}

		return new DataTableSpec(cs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (colName != null) {
			settings.addString(CFG_COLNAME, colName);
		}
		settings.addBoolean(VISUAL, visual);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		colName = settings.getString(CFG_COLNAME);
		visual = settings.getBoolean(VISUAL, false);

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
}
