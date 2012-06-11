/*
 * Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
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
package org.openscience.cdk.knime.elementfilter;

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
import org.openscience.cdk.knime.type.CDKValue;

/**
 * <code>NodeDialog</code> for the "ElementFilter" Node.
 * 
 * @author Stephan Beisken
 */
public class ElementFilterNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);
	private final JRadioButton customSetButton;
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

		panel.add(new JLabel("Standard set "), c);
		c.gridx++;
		standardSetButton = new JRadioButton("(C,H,N,O,P,S)");
		standardSetButton.setSelected(true);
		panel.add(standardSetButton, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Custom set "), c);
		c.gridx++;
		customSetButton = new JRadioButton("(comma separated)");
		customSetButton.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {

				if (customSetButton.isSelected()) {
					elementField.setEditable(true);
				} else {
					elementField.setEditable(false);
				}
			}
		});
		panel.add(customSetButton, c);
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
		buttonGroup.add(customSetButton);

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

		molColumn.update(specs[0], this.settings.getMolColumnName());
		String elementSet = this.settings.getElements();
		if (elementSet.equals(STANDARDSET)) {
			standardSetButton.setSelected(true);
			elementField.setEditable(false);
		} else {
			customSetButton.setSelected(true);
			elementField.setText(this.settings.getElements());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		this.settings.setMolColumnName(molColumn.getSelectedColumn());
		if (standardSetButton.isSelected()) {
			this.settings.setElements(STANDARDSET);
		} else {
			this.settings.setElements(elementField.getText());
		}

		this.settings.saveSettings(settings);
	}
}
