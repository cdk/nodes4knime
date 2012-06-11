/*
 * Created on 30.01.2007 17:32:33 by thor ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.connectivity;

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
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This class provides the dialog for the connectivity node. It allows choosing the molecule column and how rows with
 * fragmented should be treated.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

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

		m_molColumn.update(specs[0], m_settings.molColumnName());
		m_removeCompleteRow.setSelected(m_settings.removeCompleteRow());
		m_removeSmallParts.setSelected(!m_settings.removeCompleteRow() && !m_settings.addFragmentColumn());
		m_addFragmentColumn.setSelected(m_settings.addFragmentColumn());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.molColumnName(m_molColumn.getSelectedColumn());
		m_settings.removeCompleteRow(m_removeCompleteRow.isSelected());
		m_settings.addFragmentColumn(m_addFragmentColumn.isSelected());
		m_settings.saveSettings(settings);
	}
}
