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
package org.openscience.cdk.knime.molprops;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Dialog to choose the molecular properties to be calculated.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MolPropsNodeDialog extends NodeDialogPane {

	private final ColumnSelectionPanel m_selPanel;

	private final ColumnFilterPanel m_filterPanel;

	/**
	 * Inits GUI.
	 */
	@SuppressWarnings("unchecked")
	MolPropsNodeDialog() {

		m_selPanel = new ColumnSelectionPanel(CDKValue.class);
		m_filterPanel = new ColumnFilterPanel(false);
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.add(m_selPanel, BorderLayout.NORTH);
		panel.add(m_filterPanel, BorderLayout.CENTER);
		addTab("Properties and target column", panel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		String a = settings.getString(MolPropsNodeModel.CFGKEY_SMILES, null);
		String[] selProps = settings.getStringArray(MolPropsNodeModel.CFGKEY_PROPS, (String[]) null);
		m_selPanel.update(specs[0], a);
		DataTableSpec dummySpec = new DataTableSpec(MolPropsNodeModel.getAvailableDescriptorList());
		m_filterPanel.update(dummySpec, false, selProps);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		String smilesCell = m_selPanel.getSelectedColumn();
		settings.addString(MolPropsNodeModel.CFGKEY_SMILES, smilesCell);
		String[] selProps = m_filterPanel.getIncludedColumnSet().toArray(new String[0]);
		settings.addStringArray(MolPropsNodeModel.CFGKEY_PROPS, selProps);
	}
}
