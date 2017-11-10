/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.core;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.FilesHistoryPanel;

public class CDKDialog extends NodeDialogPane {

	/*
	 * Padding for text labels.
	 */
	public final static String PADDING = "   ";
	/*
	 * Constant to control file deletion in loops.
	 */
	public final static String TERMINUS = "loopTerminus";

	private final GridBagConstraints c;

	private final Map<String, JTextField> textField;
	private final Map<String, JComponent> customField;
	private final Map<String, JRadioButton[]> radioButtonField;
	private final Map<String, ColumnSelectionComboxBox> comboBox;
	// if the column selection combo box does not refer to the first input port
	private final Map<String, Integer> comboBoxSpec;

	/**
	 * Constructs a template dialog pane.
	 */
	public CDKDialog() {

		this.textField = new HashMap<String, JTextField>();
		this.customField = new HashMap<String, JComponent>();

		this.radioButtonField = new HashMap<String, JRadioButton[]>();
		this.comboBox = new LinkedHashMap<String, ColumnSelectionComboxBox>();
		this.comboBoxSpec = new HashMap<String, Integer>();

		this.c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
	}

	/**
	 * Adds a column selection box to the source panel.
	 * 
	 * @param label a label for the selection box
	 * @param cellValue a <code>CellValue</code> defining the selection box
	 */
	public void addColumnSelection(final String label, final Class<? extends DataValue>... cellValue) {
		addColumnSelection(label, 0, cellValue);
	}

	/**
	 * Adds a column selection box to the source panel referring to a specific data table specification.
	 * 
	 * @param label a label for the selection box
	 * @param tableSpec a data table specification index
	 * @param cellValue a <code>CellValue</code> defining the selection box
	 */
	public void addColumnSelection(final String label, final int tableSpec,
			final Class<? extends DataValue>... cellValue) {

		comboBox.put(label, new ColumnSelectionComboxBox((Border) null, cellValue));
		comboBoxSpec.put(label, tableSpec);
	}

	/**
	 * Adds a <code>JTextField</code> to the parameter panel.
	 * 
	 * @param label a label for the text field
	 * @param width a width for the text field
	 */
	public void addTextOption(final String label, final int width) {
		textField.put(label, new JTextField(width));
	}

	public void addRadioButtonOption(final String label, final JRadioButton... components) {
		radioButtonField.put(label, components);
	}

	/**
	 * Adds a component to the parameter panel. Only supports FilesHistoryPanel, JRadioButton, and JCheckBox components.
	 * 
	 * @param label a label for the component
	 * @param component a component
	 */
	public void addCustomOption(final String label, final JComponent component) {

		if (component instanceof FilesHistoryPanel || component instanceof JCheckBox)
			customField.put(label, component);
	}

	/**
	 * Builds a panel containing all node settings.
	 * 
	 * @return the panel
	 */
	public CDKDialog build() {

		JPanel panel = new JPanel(new GridLayout(2, 1));

		JPanel columnPanel = buildColumnSelection();
		JPanel optionPanel = buildTextandCustomOption();

		panel.add(columnPanel);
		panel.add(optionPanel);

		this.addTab("Settings", panel);

		return this;
	}

	/**
	 * Builds a panel with column selection options.
	 * 
	 * @return the panel
	 */
	private JPanel buildColumnSelection() {

		JPanel columnPanel = new JPanel(new GridBagLayout());
		columnPanel.setBorder(BorderFactory.createTitledBorder("Source"));

		c.gridx = 0;
		c.gridy = 0;

		c.insets = new Insets(0, 0, 5, 0);

		for (String label : comboBox.keySet()) {

			columnPanel.add(new JLabel(label + PADDING), c);
			c.gridx++;
			columnPanel.add(comboBox.get(label), c);
			c.gridx = 0;
			c.gridy++;
		}

		c.insets = new Insets(0, 0, 0, 0);

		return columnPanel;
	}

	/**
	 * Builds a panel with parameter options.
	 * 
	 * @return the panel
	 */
	private JPanel buildTextandCustomOption() {

		JPanel optionPanel = new JPanel(new GridBagLayout());
		optionPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));

		c.gridx = 0;
		c.gridy = 0;

		for (String label : textField.keySet()) {

			optionPanel.add(new JLabel(label + PADDING), c);
			c.gridx++;
			optionPanel.add(textField.get(label), c);
			c.gridx = 0;
			c.gridy++;
		}

		for (String label : radioButtonField.keySet()) {

			optionPanel.add(new JLabel(label + PADDING), c);
			c.gridx++;
			for (JRadioButton radioButton : radioButtonField.get(label)) {
				optionPanel.add(radioButton, c);
				c.gridy++;
			}
			c.gridx = 0;
		}

		for (String label : customField.keySet()) {

			optionPanel.add(new JLabel(label + PADDING), c);
			c.gridx++;
			optionPanel.add(customField.get(label), c);
			c.gridx = 0;
			c.gridy++;
		}

		c.insets = new Insets(10, 0, 0, 0);
		c.gridx = 0;
		c.gridy++;

		return optionPanel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		try {
			for (String label : comboBox.keySet())
				comboBox.get(label).update(specs[comboBoxSpec.get(label)], settings.getString(label));

			for (String label : textField.keySet())
				textField.get(label).setText(settings.getString(label));

			for (String label : radioButtonField.keySet()) {
				int index = settings.getInt(label);
				radioButtonField.get(label)[index].setSelected(true);
			}

			for (String label : customField.keySet()) {
				if (customField.get(label) instanceof FilesHistoryPanel)
					((FilesHistoryPanel) customField.get(label)).setSelectedFile(settings.getString(label));
				else if (customField.get(label) instanceof JCheckBox)
					((JCheckBox) customField.get(label)).setSelected(settings.getBoolean(label));
			}
		} catch (InvalidSettingsException exception) {
			// throw new NotConfigurableException("Error loading node settings.", exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		for (String label : comboBox.keySet())
			settings.addString(label, comboBox.get(label).getSelectedColumn());

		for (String label : textField.keySet())
			settings.addString(label, textField.get(label).getText());

		for (String label : radioButtonField.keySet()) {
			int index = 0;
			for (JRadioButton radioButton : radioButtonField.get(label)) {
				if (radioButton.isSelected()) {
					settings.addInt(label, index);
					break;
				}
				index++;
			}
		}

		for (String label : customField.keySet()) {
			if (customField.get(label) instanceof FilesHistoryPanel)
				settings.addString(label, ((FilesHistoryPanel) customField.get(label)).getSelectedFile());
			else if (customField.get(label) instanceof JCheckBox)
				settings.addBoolean(label, ((JCheckBox) customField.get(label)).isSelected());
		}
	}
}
