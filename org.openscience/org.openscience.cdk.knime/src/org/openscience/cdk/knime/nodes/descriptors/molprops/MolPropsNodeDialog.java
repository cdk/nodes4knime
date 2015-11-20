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
package org.openscience.cdk.knime.nodes.descriptors.molprops;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.openscience.cdk.knime.commons.CDKNodeUtils;

/**
 * Dialog to choose the molecular properties to be calculated.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MolPropsNodeDialog extends NodeDialogPane {

	private final ColumnSelectionPanel m_selPanel;
	private final DataColumnSpecFilterPanel m_filterPanel;

	/**
	 * Inits GUI.
	 */
	@SuppressWarnings("unchecked")
	MolPropsNodeDialog() {

		m_selPanel = new ColumnSelectionPanel(CDKNodeUtils.ACCEPTED_VALUE_CLASSES);
		m_filterPanel = new DataColumnSpecFilterPanel(false);
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
		m_selPanel.update(specs[0], a);
		
		String[] inclArr = settings.getStringArray(MolPropsNodeModel.CFGKEY_PROPS, (String[]) null);
		List<String> incl = Arrays.asList(inclArr);
		List<String> all = new ArrayList<>();
		List<String> excl = new ArrayList<>();
		for (DataColumnSpec dcs : MolPropsNodeModel.getAvailableDescriptorList()) {
			all.add(dcs.getName());
			if (!incl.contains(dcs.getName())) excl.add(dcs.getName());
		}
		m_filterPanel.update(incl, excl, all.toArray(new String[0]));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		String smilesCell = m_selPanel.getSelectedColumn();
		settings.addString(MolPropsNodeModel.CFGKEY_SMILES, smilesCell);
		String[] selProps = m_filterPanel.getIncludedNamesAsSet().toArray(new String[0]);
		settings.addStringArray(MolPropsNodeModel.CFGKEY_PROPS, selProps);
	}
}
