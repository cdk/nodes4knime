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
package org.openscience.cdk.knime.sketcher;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.openscience.cdk.knime.util.JMolSketcherPanel;

/**
 * @author wiswedel, University of Konstanz
 */
public class SketcherNodeDialogPane extends NodeDialogPane {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SketcherNodeDialogPane.class);

	private final JMolSketcherPanel m_panel = new JMolSketcherPanel();

	/**
	 * Creates a new dialog for the Molecule Sketcher node.
	 */
	public SketcherNodeDialogPane() {

		addTab("JChemPaint", m_panel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		String sdf = settings.getString(SketcherNodeModel.CFG_STRUCTURE, (String) null);
		if (sdf != null) {
			try {
				m_panel.loadStructures(sdf);
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage(), ex);
				throw new NotConfigurableException(ex.getMessage());
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		settings.addString(SketcherNodeModel.CFG_STRUCTURE, m_panel.getSDF());
	}
}
