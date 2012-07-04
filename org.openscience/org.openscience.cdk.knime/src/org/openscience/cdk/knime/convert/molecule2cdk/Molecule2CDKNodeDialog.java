/*
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.convert.molecule2cdk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.chem.types.CMLValue;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This class is the dialog for the Molecule->CDK node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class Molecule2CDKNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_colName = new ColumnSelectionComboxBox((Border) null, SdfValue.class,
			SmilesValue.class, MolValue.class, Mol2Value.class, CMLValue.class, StringValue.class);

	private final JCheckBox m_replaceColumn = new JCheckBox();

	private final JLabel m_newColNameLabel;

	private final JTextField m_newColName = new JTextField(20);

	private final JCheckBox m_generate2D = new JCheckBox();

	private final JLabel m_force2DLabel;

	private final JCheckBox m_force2D = new JCheckBox();

	private final JSpinner m_timeout = new JSpinner(new SpinnerNumberModel(10000, 0, Integer.MAX_VALUE, 10));

	private final Molecule2CDKSettings m_settings = new Molecule2CDKSettings();

	/**
	 * Creates a new dialog.
	 */
	public Molecule2CDKNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.gridx = 0;
		c.gridy = 0;
		p.add(new JLabel("Molecule column   "), c);
		c.gridx = 1;
		p.add(m_colName, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Replace column   "), c);
		c.gridx = 1;
		p.add(m_replaceColumn, c);

		c.gridy++;
		c.gridx = 0;
		m_newColNameLabel = new JLabel("   New column name   ");
		p.add(m_newColNameLabel, c);
		c.gridx = 1;
		p.add(m_newColName, c);

		m_replaceColumn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				m_newColNameLabel.setEnabled(!m_replaceColumn.isSelected());
				m_newColName.setEnabled(!m_replaceColumn.isSelected());
			}
		});

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Generate 2D coordinates   "), c);
		c.gridx = 1;
		p.add(m_generate2D, c);

		c.gridy++;
		c.gridx = 0;
		m_force2DLabel = new JLabel("   Force generation   ");
		p.add(m_force2DLabel, c);
		c.gridx = 1;
		p.add(m_force2D, c);

		m_generate2D.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				m_force2DLabel.setEnabled(m_generate2D.isSelected());
				m_force2D.setEnabled(m_generate2D.isSelected());
			}
		});

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Processing timeout   "), c);
		c.gridx = 1;
		p.add(m_timeout, c);

		addTab("Standard settings", p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		m_settings.loadSettingsForDialog(settings);

		m_colName.update(specs[0], m_settings.columnName());
		m_replaceColumn.setSelected(m_settings.replaceColumn());
		m_newColNameLabel.setEnabled(!m_settings.replaceColumn());
		m_newColName.setEnabled(!m_settings.replaceColumn());
		m_newColName.setText(m_settings.newColumnName() != null ? m_settings.newColumnName() : "");

		m_generate2D.setSelected(m_settings.generate2D());
		m_force2DLabel.setEnabled(m_settings.generate2D());
		m_force2D.setEnabled(m_settings.generate2D());
		m_force2D.setSelected(m_settings.force2D());

		m_timeout.setValue(m_settings.timeout());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.columnName(m_colName.getSelectedColumn());
		m_settings.replaceColumn(m_replaceColumn.isSelected());
		m_settings.newColumnName(m_newColName.getText());
		m_settings.generate2D(m_generate2D.isSelected());
		m_settings.force2D(m_force2D.isSelected());
		m_settings.timeout((Integer) m_timeout.getValue());
		m_settings.saveSettings(settings);
	}
}
