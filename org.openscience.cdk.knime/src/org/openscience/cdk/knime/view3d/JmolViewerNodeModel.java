/*
 * Copyright (C) 2003 - 2016 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.tableview.TableContentModel;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;

/**
 * @author Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class JmolViewerNodeModel extends CDKAdapterNodeModel implements BufferedDataTableHolder {

	private final TableContentModel m_contentModel;

	/** Public constructor */
	public JmolViewerNodeModel() {

		super(1, 0, new JmolViewerSettings());
		m_contentModel = new TableContentModel();
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
	 * Returns the node settings.
	 * 
	 * @return the settings object
	 */
	public JmolViewerSettings getSettings() {
		return settings(JmolViewerSettings.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		this.settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {

		JmolViewerSettings s = new JmolViewerSettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() < 1)) {
			throw new InvalidSettingsException("No column choosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		this.settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		setInternalTables(convertedTables);
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

		autoConfigure(inSpecs);
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
}
