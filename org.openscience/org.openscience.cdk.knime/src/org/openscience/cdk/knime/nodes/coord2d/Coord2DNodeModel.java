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
package org.openscience.cdk.knime.nodes.coord2d;

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
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This class is the model for the CDK 2D-generation node. It takes the input molecules (if there are any), checks if
 * they already have 2D coordinates assigned and if not creates a new copy of the molecule and computes 2D coordinates
 * for it. The columns with molecules will have a property afterwards, that indicates that 3D coordinates are available
 * ( {@link CDKCell#COORD2D_AVAILABLE}).
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class Coord2DNodeModel extends CDKAdapterNodeModel {

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";
	static final String FORCE = "force";

	private String m_colName;
	private boolean m_force;

	/**
	 * Creates a new model for 2D coordinate generation.
	 */
	public Coord2DNodeModel() {
		super(1, 1, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		m_colName = CDKNodeUtils.autoConfigure(inSpecs[0], m_colName);
		columnIndex = inSpecs[0].findColumnIndex(m_colName);
		DataTableSpec outSpec = convertTables(inSpecs)[0];
		return new DataTableSpec[] { appendSpec(outSpec) };
	}

	private DataTableSpec appendSpec(DataTableSpec spec) {

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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_colName = settings.getString(CFG_COLNAME);
		m_force = settings.getBoolean(FORCE, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (m_colName != null) {
			settings.addString(CFG_COLNAME, m_colName);
		}
		settings.addBoolean(FORCE, m_force);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTable = exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec()));

		Coord2DWorker worker = new Coord2DWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec, outputTable,
				m_force);

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
}
