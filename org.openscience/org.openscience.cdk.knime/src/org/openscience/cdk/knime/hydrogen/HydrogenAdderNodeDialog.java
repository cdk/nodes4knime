/*
 * Created on 30.01.2007 14:59:26 by thor ------------------------------------------------------------------------
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
package org.openscience.cdk.knime.hydrogen;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This class is the dialog for the hydrogen adder node. It lets the user choose the column containing the molecules and
 * how the hydrogens should be added (implicitly or explicitly).
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, EMBL-EBI
 */
public class HydrogenAdderNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);
	private final JCheckBox m_appendColumnChecker = new JCheckBox("Append Column");
	private final JTextField m_appendColumnName = new JTextField(8);

	private final HydrogenAdderSettings m_settings = new HydrogenAdderSettings();

	/**
	 * Creates a new dialog.
	 */
	public HydrogenAdderNodeDialog() {

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
		p.add(m_appendColumnChecker, c);
		c.gridx = 1;
		p.add(m_appendColumnName, c);

		m_appendColumnChecker.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {

				if (m_appendColumnChecker.isSelected()) {
					m_appendColumnName.setEnabled(true);
					if ("".equals(m_appendColumnName.getText())) {
						m_appendColumnName.setText(m_molColumn.getSelectedColumn() + " (H)");
					}
				} else {
					m_appendColumnName.setEnabled(false);
				}

			}
		});
		m_appendColumnName.setEnabled(m_appendColumnChecker.isSelected());

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
		m_appendColumnChecker.setSelected(!m_settings.replaceColumn());
		if (m_settings.replaceColumn()) {
			m_appendColumnChecker.setSelected(false);
			m_appendColumnName.setText("");
		} else {
			m_appendColumnChecker.setSelected(true);
			String name = m_settings.appendColumnName();
			if (name != null && name.length() > 0) {
				// otherwise it will have a meaningful default.
				m_appendColumnName.setText(name);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.molColumnName(m_molColumn.getSelectedColumn());
		m_settings.replaceColumn(!m_appendColumnChecker.isSelected());
		m_settings.appendColumnName(m_appendColumnChecker.isSelected() ? m_appendColumnName.getText() : null);
		m_settings.saveSettings(settings);
	}
}
