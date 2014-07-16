/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.sumformula;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * <code>NodeDialog</code> for the "SumFormula" Node. Node to generate probable molecular formulas based on a given mass
 * input.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SumFormulaNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox massColumn = new ColumnSelectionComboxBox((Border) null, DoubleValue.class);

	private final JRadioButton customRemoveButton;
	private final JRadioButton customSetButton;
	private final JRadioButton allSetButton;
	private final JTextField elementField;
	private final JTextField toleranceField;
	
	private final JCheckBox ratioRule = new JCheckBox("", true);
	private final JCheckBox nitrogenRule = new JCheckBox("", true);
	private final JCheckBox numberRule = new JCheckBox("", true);
	
	private final JComboBox ratioBoxRange = new JComboBox(new Object[] {
			"Common Range", "Extended Range", "Extreme Range"
	});
	private final JComboBox ratioBoxType = new JComboBox(new Object[] {
			"H/C", "SiNOPSBrClF/C", "HSiNOPSBrClF/C"
	});
	private final JComboBox numberBox = new JComboBox(new Object[] {
			"DNP-500", "DNP-1000", "DNP-2000", "DNP-3000", "Wiley-500", "Wiley-1000", "Wiley-2000"
	});

	private SumFormulaSettings settings = new SumFormulaSettings();

	/**
	 * New pane for configuring the SumFormula node.
	 */
	protected SumFormulaNodeDialog() {

		GridBagConstraints c = new GridBagConstraints();

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Settings"));

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		panel.add(new JLabel("Mass column  "), c);
		c.gridx++;
		panel.add(massColumn, c);
		c.gridy++;
		c.gridx = 0;

		allSetButton = new JRadioButton();
		allSetButton.setEnabled(false);
		allSetButton.setSelected(false);
		allSetButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (allSetButton.isSelected()) {
					elementField.setEditable(false);
				}
			}
		});
		panel.add(new JLabel("All elements  "), c);
		c.gridx++;
		panel.add(allSetButton, c);
		c.gridy++;
		c.gridx = 0;
		
		customSetButton = new JRadioButton();
		customSetButton.setSelected(true);
		customSetButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (customSetButton.isSelected()) {
					elementField.setEditable(true);
				}
			}
		});
		panel.add(new JLabel("Include elements  "), c);
		c.gridx++;
		panel.add(customSetButton, c);
		c.gridy++;
		c.gridx = 0;
		
		customRemoveButton = new JRadioButton();
		customRemoveButton.setEnabled(false);
		customRemoveButton.setSelected(false);
		customRemoveButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (customRemoveButton.isSelected()) {
					elementField.setEditable(true);
				}
			}
		});
		panel.add(new JLabel("Exclude elements  "), c);
		c.gridx++;
		panel.add(customRemoveButton, c);
		c.gridy++;
		c.gridx = 0;
		
		elementField = new JTextField("C,H,N,O", 12);
		elementField.setEditable(true);
		panel.add(new JLabel("Elements  "), c);
		c.gridx++;
		panel.add(elementField, c);
		c.gridy++;
		c.gridx = 0;
		
		toleranceField = new JTextField("0.5", 12);
		panel.add(new JLabel("Mass tolerance  "), c);
		c.gridx++;
		panel.add(toleranceField, c);
		c.gridy++;
		c.gridx = 0;
		
		panel.add(new JLabel("Apply nitrogen rule   "), c);
		c.gridx++;
		panel.add(nitrogenRule, c);
		c.gridy++;
		c.gridx = 0;
		
		panel.add(new JLabel("Apply element ratio rule   "), c);
		c.gridx++;
		panel.add(ratioRule, c);
		c.gridy++;
		panel.add(ratioBoxType, c);
		c.gridy++;
		panel.add(ratioBoxRange, c);
		c.gridy++;
		c.gridx = 0;
		
		ratioRule.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (ratioRule.isSelected()) {
					ratioBoxType.setEnabled(true);
					ratioBoxRange.setEnabled(true);
				} else {
					ratioBoxType.setEnabled(false);
					ratioBoxRange.setEnabled(false);
				}
			}
		});
		
		panel.add(new JLabel("Apply element restrictions   "), c);
		c.gridx++;
		panel.add(numberRule, c);
		c.gridy++;
		panel.add(numberBox, c);
		c.gridy++;
		c.gridx = 0;
		
		numberRule.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (numberRule.isSelected()) {
					numberBox.setEnabled(true);
				} else {
					numberBox.setEnabled(false);
				}
			}
		});

		ButtonGroup bg = new ButtonGroup();
		bg.add(allSetButton);
		bg.add(customSetButton);
		bg.add(customRemoveButton);
		
		this.addTab("Settings", panel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		try {
			this.settings.loadSettings(settings);
		} catch (InvalidSettingsException ex) {
			// ignore it
		}

		massColumn.update(specs[0], this.settings.targetColumn());
		elementField.setText(this.settings.elements());
		if (this.settings.incAll()) {
			allSetButton.setSelected(true);
		} else if (this.settings.incSpec()) {
			customSetButton.setSelected(true);
		} else {
			customRemoveButton.setSelected(true);
		}
		toleranceField.setText(this.settings.tolerance() + "");
		nitrogenRule.setSelected(this.settings.isApplyNitrogenRule());
		
		if (!this.settings.isApplyRatioRule().isEmpty()) {
			ratioRule.setSelected(true);
			String[] els = this.settings.isApplyRatioRule().split("-");
			ratioBoxType.setSelectedItem(els[0]);
			ratioBoxRange.setSelectedItem(els[1]);
		} else {
			ratioRule.setSelected(false);
		}
		
		if (!this.settings.isApplyNumberRule().isEmpty()) {
			numberRule.setSelected(true);
			numberBox.setSelectedItem(this.settings.isApplyNumberRule());
		} else {
			numberRule.setSelected(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		this.settings.targetColumn(massColumn.getSelectedColumn());
		this.settings.elements(elementField.getText());
		this.settings.incAll(allSetButton.isSelected());
		this.settings.incSpec(customSetButton.isSelected());
		this.settings.tolerance(Double.parseDouble(toleranceField.getText()));
		this.settings.setApplyNitrogenRule(nitrogenRule.isSelected());
		this.settings.setApplyRatioRule(ratioRule.isSelected() ? ratioBoxType.getSelectedItem().toString() + 
				"-" + ratioBoxRange.getSelectedItem().toString(): "");
		this.settings.setApplyNumberRule(numberRule.isSelected() ? numberBox.getSelectedItem().toString() : "");

		this.settings.saveSettings(settings);
	}
}
