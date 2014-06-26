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
package org.openscience.cdk.knime.convert.cdk2molecule;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.chem.types.CMLValue;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.convert.cdk2molecule.CDK2MoleculeSettings.Format;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This class is the dialog for the CDK->Molecule converter node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDK2MoleculeNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

	private final JComboBox<Format> m_destFormat = new JComboBox<Format>(new Format[] { Format.SDF, Format.Smiles, Format.Mol2 });

	private final JCheckBox m_replaceColumn = new JCheckBox();

	private final JLabel m_newColNameLabel = new JLabel("   New column name   ");

	private final JTextField m_newColName = new JTextField(20);

	private final CDK2MoleculeSettings m_settings = new CDK2MoleculeSettings();

	/**
	 * Creates a new dialog.
	 */
	public CDK2MoleculeNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(2, 2, 2, 2);
		c.gridx = 0;
		c.gridy = 0;
		p.add(new JLabel("CDK column   "), c);
		c.gridx = 1;
		p.add(m_molColumn, c);

		c.gridx = 0;
		c.gridy++;
		p.add(new JLabel("Replace column   "), c);
		c.gridx = 1;
		p.add(m_replaceColumn, c);

		m_replaceColumn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				m_newColNameLabel.setEnabled(!m_replaceColumn.isSelected());
				m_newColName.setEnabled(!m_replaceColumn.isSelected());
			}
		});

		c.gridx = 0;
		c.gridy++;
		p.add(m_newColNameLabel, c);
		c.gridx = 1;
		p.add(m_newColName, c);

		c.gridx = 0;
		c.gridy++;
		p.add(new JLabel("Destination format   "), c);
		c.gridx = 1;
		p.add(m_destFormat, c);

		m_destFormat.setRenderer(new DefaultListCellRenderer() {

			@SuppressWarnings("deprecation")
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {

				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value == Format.SDF) {
					setIcon(SdfValue.UTILITY.getIcon());
					setText("SDF");
				} else if (value == Format.Mol2) {
					setIcon(Mol2Value.UTILITY.getIcon());
					setText("Mol2");
				} else if (value == Format.Smiles) {
					setIcon(SmilesValue.UTILITY.getIcon());
					setText("Smiles");
				} else if (value == Format.CML) {
					setIcon(CMLValue.UTILITY.getIcon());
					setText("CML");
				} else {
					setIcon(null);
					setText("");
				}
				return this;
			}
		});

		addTab("Standard settings", p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		m_settings.loadSettingsForDialog(settings);

		m_molColumn.update(specs[0], m_settings.columnName());
		m_replaceColumn.setSelected(m_settings.replaceColumn());
		m_newColNameLabel.setEnabled(!m_settings.replaceColumn());
		m_newColName.setEnabled(!m_settings.replaceColumn());
		m_newColName.setText(m_settings.newColumnName());
		m_destFormat.setSelectedItem(m_settings.destFormat());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.columnName(m_molColumn.getSelectedColumn());
		m_settings.replaceColumn(m_replaceColumn.isSelected());
		m_settings.newColumnName(m_newColName.getText());
		m_settings.destFormat((Format) m_destFormat.getSelectedItem());
		m_settings.saveSettings(settings);
	}
}
