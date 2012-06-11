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
 * @author Stephan Beisken
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
