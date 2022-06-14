/*
 * Copyright (C) 2003 - 2016 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.nodes.sssearch;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.util.JMolSketcherPanel;

/**
 * This is the dialog for the substructre search node. It lets the user draw a fragment and choose the column containing
 * the molecules.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SSSearchNodeDialog extends NodeDialogPane {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SSSearchNodeDialog.class);

	private final JMolSketcherPanel m_panel = new JMolSketcherPanel();
	private final JCheckBox highlightBox = new JCheckBox();
	private final JCheckBox chargeBox = new JCheckBox();
	private final JCheckBox stereoBox = new JCheckBox();

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumnName = new ColumnSelectionComboxBox((Border) null,
			CDKNodeUtils.ACCEPTED_VALUE_CLASSES);

	private SSSearchSettings m_settings = new SSSearchSettings();

	/**
	 * Creates a new dialog.
	 */
	public SSSearchNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 4;
		p.add(m_panel, c);

		c.gridy++;
		c.ipady = 5;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridwidth = 1;
		p.add(new JLabel("Column with molecules   "), c);
		c.gridx = 1;
		p.add(m_molColumnName, c);

		c.gridx = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(new JLabel("         Highlight substructures   "), c);
		c.gridx = 3;
		p.add(highlightBox, c);
		
		c.gridy++;
		c.gridx = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(new JLabel("         Match atom charges   "), c);
		c.gridx = 3;
		p.add(chargeBox, c);
		
		c.gridy++;
		c.gridx = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(new JLabel("         Exact stereo match   "), c);
		c.gridx = 3;
		p.add(stereoBox, c);

		addTab("JChemPaint", p);
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
			// ignore it and use defaults
		}

		if (m_settings.getSmiles() != null) {
			try {
				String smiles = m_settings.getSmiles();
				
				if(!smiles.isBlank())
					m_panel.loadStructures(m_settings.getSmiles());
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage(), ex);
				throw new NotConfigurableException(ex.getMessage());
			}
		}

		m_molColumnName.update(specs[0], m_settings.targetColumn());

		highlightBox.setSelected(m_settings.isHighlight());
		chargeBox.setSelected(m_settings.isCharge());
		stereoBox.setSelected(m_settings.isExactMatch());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		try {
			if (m_panel.getSmiles().length() == 0) {
				m_settings.setSmiles("");
			} else {
				m_settings.setSmiles(m_panel.getSmiles());
			}
		} catch (Exception exception) {
			// ignore JChemPaint madness
		}
		m_settings.targetColumn(m_molColumnName.getSelectedColumn());
		m_settings.setHighlight(highlightBox.isSelected());
		m_settings.setCharge(chargeBox.isSelected());
		m_settings.setExactMatch(stereoBox.isSelected());
		m_settings.saveSettings(settings);
	}
}
