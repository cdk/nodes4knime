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
package org.openscience.cdk.knime.nodes.coord3d;

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
 * This is the model implementation of Coord3d. Integrates the CDK 3D Model Builder to calculate 3D coordinates for CDK
 * molecules.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Coord3dNodeModel extends CDKAdapterNodeModel {

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";
	static final String TIMEOUT = "timeout";

	private String m_colName;
	private int timeout = 10000;

	/**
	 * Creates a new model for 3D coordinate generation.
	 */
	public Coord3dNodeModel() {
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
		timeout = settings.getInt(TIMEOUT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (m_colName != null) {
			settings.addString(CFG_COLNAME, m_colName);
			settings.addInt(TIMEOUT, timeout);
		}
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

		Coord3dWorker worker = new Coord3dWorker(1, 1, columnIndex, exec, outputTable,
				timeout);

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
