/*
 * Copyright (c) 2012, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.atomsignature;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
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
import org.openscience.cdk.knime.atomsignature.AtomSignatureSettings.AtomTypes;
import org.openscience.cdk.knime.atomsignature.AtomSignatureSettings.SignatureTypes;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * <code>NodeDialog</code> for the "AtomSignature" Node.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class AtomSignatureNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

	private final AtomSignatureSettings m_settings = new AtomSignatureSettings();
	private final JRadioButton m_hosecodes = new JRadioButton("Hose Codes");
	private final JRadioButton m_atomsignature = new JRadioButton("Atom Signatures");

	private final JRadioButton m_protons = new JRadioButton("H");
	private final JRadioButton m_carbons = new JRadioButton("C");

	private final JCheckBox m_heightChecker = new JCheckBox("Set signature height");
	private final JTextField m_minHeight = new JTextField(8);
	private final JTextField m_maxHeight = new JTextField(8);

	/**
	 * New pane for configuring AtomSignature node dialog. This is just a suggestion to demonstrate possible default
	 * dialog components.
	 */
	public AtomSignatureNodeDialog() {

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
		p.add(new JLabel("Signature type   "), c);
		c.gridx = 1;
		p.add(m_hosecodes, c);
		c.gridy++;
		p.add(m_atomsignature, c);

		c.gridy++;
		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Atom of interest   "), c);
		c.gridx = 1;
		p.add(m_protons, c);
		c.gridy++;
		p.add(m_carbons, c);

		ButtonGroup bg = new ButtonGroup();
		bg.add(m_hosecodes);
		bg.add(m_atomsignature);

		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(m_protons);
		bg2.add(m_carbons);

		c.gridy++;
		c.gridx = 0;
		p.add(m_heightChecker, c);
		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("   Min height:"), c);
		c.gridx = 1;
		p.add(m_minHeight, c);
		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("   Max height:"), c);
		c.gridx = 1;
		p.add(m_maxHeight, c);

		// add a listener for the checkbox
		// if selected then process information
		m_heightChecker.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {

				// if box ticked
				if (m_heightChecker.isSelected()) {
					m_minHeight.setEnabled(true);
					m_maxHeight.setEnabled(true);
					// set the information in the textbox?
					if ("".equals(m_minHeight.getText())) {
						m_minHeight.setText("1");
					}
					if ("".equals(m_maxHeight.getText())) {
						m_maxHeight.setText("6");
					}
				} else {
					m_minHeight.setEnabled(false);
					m_minHeight.setText("");
					m_maxHeight.setEnabled(false);
					m_maxHeight.setText("");

				}

			}
		});

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

		if (m_settings.signatureType().equals(SignatureTypes.Hose)) {
			m_hosecodes.setSelected(true);
		} else if (m_settings.signatureType().equals(SignatureTypes.AtomSignatures)) {
			m_atomsignature.setSelected(true);
		}

		if (m_settings.atomType().equals(AtomTypes.H)) {
			m_protons.setSelected(true);
		} else if (m_settings.atomType().equals(AtomTypes.C)) {
			m_carbons.setSelected(true);
		}
		if (m_settings.isHeightSet()) {
			m_heightChecker.setSelected(true);
			if (m_minHeight.isEnabled())
				m_minHeight.setText(Integer.toString(m_settings.getMinHeight()));
			if (m_maxHeight.isEnabled())
				m_maxHeight.setText(Integer.toString(m_settings.getMaxHeight()));
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.molColumnName(m_molColumn.getSelectedColumn());
		// validate the min and max height values
		if (m_heightChecker.isSelected()) {
			m_settings.heightSet(m_heightChecker.isSelected());
			try {
				Integer min_height = Integer.parseInt(m_minHeight.getText());
				Integer max_height = Integer.parseInt(m_maxHeight.getText());
				if (max_height != null & min_height != null) {
					m_settings.minHeight(m_heightChecker.isSelected() ? (int) min_height : 6);
					m_settings.maxHeight(m_heightChecker.isSelected() ? (int) max_height : 6);
				}
			} catch (NumberFormatException nfe) {
				/*
				 * handle the case where the textfield does not contain a number, e.g. show a warning or change the
				 * background or whatever you see fit.
				 */
			}
		} else {
			m_settings.heightSet(false);
			m_settings.minHeight(6);
			m_settings.maxHeight(6);
		}

		m_settings.setAtomType(m_carbons.isSelected() ? AtomTypes.C : AtomTypes.H);
		m_settings.setSignatureType(m_atomsignature.isSelected() ? SignatureTypes.AtomSignatures : SignatureTypes.Hose);

		m_settings.saveSettings(settings);
	}
}
