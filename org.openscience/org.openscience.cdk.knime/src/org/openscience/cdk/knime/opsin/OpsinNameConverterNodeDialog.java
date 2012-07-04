/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.opsin;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * <code>NodeDialog</code> for the "OpsinNameConverter" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class OpsinNameConverterNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox nameColumn = new ColumnSelectionComboxBox((Border) null, StringValue.class);
	private final JCheckBox cdkBox = new JCheckBox("", true);
	private final JCheckBox smilesBox = new JCheckBox();
	private final JCheckBox inchiBox = new JCheckBox();
	private final JCheckBox pngBox = new JCheckBox();
	private final JCheckBox cmlBox = new JCheckBox();

	private OpsinNameConverterSettings settings = new OpsinNameConverterSettings();

	/**
	 * New pane for configuring the OpsinNameConverter node.
	 */
	protected OpsinNameConverterNodeDialog() {

		GridBagConstraints c = new GridBagConstraints();

		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Settings"));

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		panel.add(new JLabel("IUPAC string column  "), c);
		c.gridx++;
		panel.add(nameColumn, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Append CDK molecule column  "), c);
		c.gridx++;
		panel.add(cdkBox, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Append SMILES column  "), c);
		c.gridx++;
		panel.add(smilesBox, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Append InChI column  "), c);
		c.gridx++;
		panel.add(inchiBox, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Append PNG column  "), c);
		c.gridx++;
		panel.add(pngBox, c);
		c.gridy++;
		c.gridx = 0;

		panel.add(new JLabel("Append CML column  "), c);
		c.gridx++;
		panel.add(cmlBox, c);
		c.gridy++;
		c.gridx = 0;

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

		nameColumn.update(specs[0], this.settings.getNameColumn());
		cmlBox.setSelected(this.settings.isAddCml());
		smilesBox.setSelected(this.settings.isAddSmiles());
		cdkBox.setSelected(this.settings.isAddCdk());
		inchiBox.setSelected(this.settings.isAddInChI());
		pngBox.setSelected(this.settings.isAddPng());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		this.settings.setNameColumn(nameColumn.getSelectedColumn());
		this.settings.setAddCdk(cdkBox.isSelected());
		this.settings.setAddSmiles(smilesBox.isSelected());
		this.settings.setAddCml(cmlBox.isSelected());
		this.settings.setAddInChI(inchiBox.isSelected());
		this.settings.setAddPng(pngBox.isSelected());

		this.settings.saveSettings(settings);
	}
}
