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
package org.openscience.cdk.knime.nodes.elementfilter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
 * <code>NodeDialog</code> for the "ElementFilter" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ElementFilterNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox molColumn = new ColumnSelectionComboxBox((Border) null,
			CDKNodeUtils.ACCEPTED_VALUE_CLASSES);
	private final JRadioButton customSetRemoveButton;
	private final JRadioButton customSetKeepButton;
	private final JRadioButton standardSetButton;
	private final JTextField elementField;

	private static final String STANDARDSET = "C,H,N,O,P,S";

	private ElementFilterSettings settings = new ElementFilterSettings();

	/**
	 * New pane for configuring the ElementFilter node.
	 */
	protected ElementFilterNodeDialog() {

		GridBagConstraints c = new GridBagConstraints();

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Settings"));

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		panel.add(new JLabel("CDK column "), c);
		c.gridx++;
		panel.add(molColumn, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Standard set (keep) "), c);
		c.gridx++;
		standardSetButton = new JRadioButton("(C,H,N,O,P,S)");
		standardSetButton.setSelected(true);
		standardSetButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (standardSetButton.isSelected()) {
					elementField.setEditable(false);
				}
			}
		});
		panel.add(standardSetButton, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Custom set (keep) "), c);
		c.gridx++;
		customSetKeepButton = new JRadioButton("(comma separated)");
		customSetKeepButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (customSetKeepButton.isSelected()) {
					elementField.setEditable(true);
				}
			}
		});
		panel.add(customSetKeepButton, c);
		c.gridy++;
		c.gridx = 0;
		
		panel.add(new JLabel("Custom set (remove) "), c);
		c.gridx++;
		customSetRemoveButton = new JRadioButton("(comma separated)");
		customSetRemoveButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				if (customSetRemoveButton.isSelected()) {
					elementField.setEditable(true);
				}
			}
		});
		panel.add(customSetRemoveButton, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Element string  "), c);
		c.gridx++;
		elementField = new JTextField(15);
		elementField.setEditable(false);
		panel.add(elementField, c);
		c.gridy++;
		c.gridx = 0;

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(standardSetButton);
		buttonGroup.add(customSetKeepButton);
		buttonGroup.add(customSetRemoveButton);

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

		molColumn.update(specs[0], this.settings.targetColumn());
		String elementSet = this.settings.getElements();
		if (elementSet.equals(STANDARDSET) && this.settings.getKeep()) {
			standardSetButton.setSelected(true);
			elementField.setEditable(false);
		} else if (this.settings.getKeep()) {
			customSetKeepButton.setSelected(true);
			elementField.setText(this.settings.getElements());
		} else {
			customSetRemoveButton.setSelected(true);
			elementField.setText(this.settings.getElements());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		this.settings.targetColumn(molColumn.getSelectedColumn());
		if (standardSetButton.isSelected()) {
			this.settings.setElements(STANDARDSET);
			this.settings.setKeep(true);
		} else {
			this.settings.setKeep(customSetKeepButton.isSelected());
			this.settings.setElements(elementField.getText());
		}

		this.settings.saveSettings(settings);
	}
}
