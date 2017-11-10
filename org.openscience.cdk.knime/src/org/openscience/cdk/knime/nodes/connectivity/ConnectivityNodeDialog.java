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
package org.openscience.cdk.knime.nodes.connectivity;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.commons.CDKNodeUtils;

/**
 * This class provides the dialog for the connectivity node. It allows choosing the molecule column and how rows with
 * fragmented should be treated.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null,
			CDKNodeUtils.ACCEPTED_VALUE_CLASSES);

	private final JRadioButton m_removeCompleteRow = new JRadioButton();

	private final JRadioButton m_removeSmallParts = new JRadioButton();

	private final JRadioButton m_addFragmentColumn = new JRadioButton();

	private final ConnectivitySettings m_settings = new ConnectivitySettings();

	/**
	 * Creates a new dialog.
	 */
	public ConnectivityNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		p.add(new JLabel("Column with molecules   "), c);
		c.gridx++;
		p.add(m_molColumn, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Remove complete row "), c);
		c.gridx = 1;
		p.add(m_removeCompleteRow, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Leave only biggest fragment "), c);
		c.gridx = 1;
		p.add(m_removeSmallParts, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Add column with all fragments "), c);
		c.gridx = 1;
		p.add(m_addFragmentColumn, c);

		ButtonGroup bg = new ButtonGroup();
		bg.add(m_removeCompleteRow);
		bg.add(m_removeSmallParts);
		bg.add(m_addFragmentColumn);

		addTab("Default settings", p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		try {
			m_settings.loadSettings(settings);
		} catch (InvalidSettingsException ex) {
			// ignore it
		}

		m_molColumn.update(specs[0], m_settings.targetColumn());
		m_removeCompleteRow.setSelected(m_settings.removeCompleteRow());
		m_removeSmallParts.setSelected(!m_settings.removeCompleteRow() && !m_settings.addFragmentColumn());
		m_addFragmentColumn.setSelected(m_settings.addFragmentColumn());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.targetColumn(m_molColumn.getSelectedColumn());
		m_settings.removeCompleteRow(m_removeCompleteRow.isSelected());
		m_settings.addFragmentColumn(m_addFragmentColumn.isSelected());
		m_settings.saveSettings(settings);
	}
}
