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
package org.openscience.cdk.knime.nodes.hydrogen;

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
import org.openscience.cdk.knime.commons.CDKNodeUtils;

/**
 * This class is the dialog for the hydrogen adder node. It lets the user choose the column containing the molecules and
 * how the hydrogens should be added (implicitly or explicitly).
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class HydrogenAdderNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null,
			CDKNodeUtils.ACCEPTED_VALUE_CLASSES);
	private final JCheckBox m_appendColumnChecker = new JCheckBox("Append Column");
	private final JTextField m_appendColumnName = new JTextField(8);
	private final JCheckBox exludeStereo = new JCheckBox();

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
		p.add(new JLabel("Exclude 'stereo hydrogens'   "), c);
		c.gridx++;
		p.add(exludeStereo, c);

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

		m_molColumn.update(specs[0], m_settings.targetColumn());
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

		exludeStereo.setSelected(m_settings.excludeStereo());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.targetColumn(m_molColumn.getSelectedColumn());
		m_settings.excludeStereo(exludeStereo.isSelected());
		m_settings.replaceColumn(!m_appendColumnChecker.isSelected());
		m_settings.appendColumnName(m_appendColumnChecker.isSelected() ? m_appendColumnName.getText() : null);
		m_settings.saveSettings(settings);
	}
}
