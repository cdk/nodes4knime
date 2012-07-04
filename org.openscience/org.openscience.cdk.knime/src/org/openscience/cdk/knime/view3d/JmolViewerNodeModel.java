/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.view3d;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.tableview.TableContentModel;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * @author Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class JmolViewerNodeModel extends NodeModel implements BufferedDataTableHolder {
	
	private final TableContentModel m_contentModel;

	/** Config key for column name. */
	static final String CFG_COLNAME = "colName";
	static final String COL_INDEX = "colIndex";
	private String colName;
	private int colIndex = -1;

	/** Public constructor */
	public JmolViewerNodeModel() {

		super(1, 0);
		m_contentModel = new TableContentModel();
	}

	/**
	 * Get the index of the selected structure column.
	 * 
	 * @return The structure column (SDF or CDK) or -1 if none has been selected.
	 */
	int getStructureColumn() {

		return colIndex;
	}

	/**
	 * Get reference to the table model.
	 * 
	 * @return The table model to be displayed on top.
	 */
	TableContentModel getContentModel() {

		return m_contentModel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {

		settings.addString(CFG_COLNAME, colName);
		settings.addInt(COL_INDEX, colIndex);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {

		String column = settings.getString(CFG_COLNAME);
		if ((column == null) || (column.length() == 0)) {
			throw new InvalidSettingsException("No CDK molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {

		colName = settings.getString(CFG_COLNAME);
		colIndex = settings.getInt(COL_INDEX);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec) throws Exception {

		setInternalTables(inData);
		return new BufferedDataTable[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		m_contentModel.setDataTable(null);
		m_contentModel.setHiLiteHandler(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int molColIndex = inSpecs[0].findColumnIndex(colName);
		if (molColIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
				if (spec.getType().isCompatible(CDKValue.class)) {
					if (molColIndex != -1) {
						setWarningMessage("Column '" + spec.getName() + "' automatically chosen as molecule column");
					}
					molColIndex = i;
					colName = spec.getName();
				}
				i++;
			}

			if (molColIndex == -1) {
				throw new InvalidSettingsException("Column '" + colName + "' does not exist");
			}
		}

		if (!inSpecs[0].getColumnSpec(molColIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column '" + colName + "' does not contain CDK cells");
		}
		
		colIndex = molColIndex;

		return null;
	}

	/** {@inheritDoc} */
	@Override
	public BufferedDataTable[] getInternalTables() {

		return new BufferedDataTable[] { (BufferedDataTable) m_contentModel.getDataTable() };
	}

	/** {@inheritDoc} */
	@Override
	public void setInternalTables(final BufferedDataTable[] tables) {

		m_contentModel.setDataTable(tables[0]);
		m_contentModel.setHiLiteHandler(getInHiLiteHandler(0));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}
}
