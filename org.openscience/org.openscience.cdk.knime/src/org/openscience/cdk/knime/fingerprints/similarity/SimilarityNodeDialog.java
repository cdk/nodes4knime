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
package org.openscience.cdk.knime.fingerprints.similarity;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.AggregationMethod;

/**
 * <code>NodeDialog</code> for the "Similarity" Node.
 * 
 * @author Stephan Beisken
 */
public class SimilarityNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_fingerprintColumn = new ColumnSelectionComboxBox((Border) null,
			BitVectorValue.class);
	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_fingerprintRefColumn = new ColumnSelectionComboxBox((Border) null,
			BitVectorValue.class);

	private final JRadioButton m_minimum = new JRadioButton("Minimum");
	private final JRadioButton m_maximum = new JRadioButton("Maximum");
	private final JRadioButton m_average = new JRadioButton("Average");

	private final SimilaritySettings m_settings = new SimilaritySettings();

	/**
	 * New pane for configuring the Similarity node.
	 */
	protected SimilarityNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		p.add(new JLabel("Column with fingerprints   "), c);
		c.gridx = 1;
		p.add(m_fingerprintColumn, c);
		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Column with reference fingerprints   "), c);
		c.gridx = 1;
		p.add(m_fingerprintRefColumn, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Aggregation method   "), c);
		c.gridx = 1;
		p.add(m_minimum, c);
		c.gridy++;
		p.add(m_maximum, c);
		m_maximum.setSelected(true);
		c.gridy++;
		p.add(m_average, c);

		ButtonGroup bg1 = new ButtonGroup();
		bg1.add(m_minimum);
		bg1.add(m_maximum);
		bg1.add(m_average);

		addTab("Similarity Options", p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		try {
			m_settings.loadSettings(settings);
		} catch (InvalidSettingsException exception) {
			// ignore it
		}

		m_fingerprintColumn.update(specs[0], m_settings.fingerprintColumn());
		m_fingerprintRefColumn.update(specs[1], m_settings.fingerprintRefColumn());

		if (m_settings.aggregationMethod().equals(AggregationMethod.Minimum)) {
			m_minimum.setSelected(true);
		} else if (m_settings.aggregationMethod().equals(AggregationMethod.Maximum)) {
			m_maximum.setSelected(true);
		} else if (m_settings.aggregationMethod().equals(AggregationMethod.Average)) {
			m_average.setSelected(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		m_settings.fingerprintColumn(m_fingerprintColumn.getSelectedColumn());
		m_settings.fingerprintRefColumn(m_fingerprintRefColumn.getSelectedColumn());
		if (m_minimum.isSelected()) {
			m_settings.aggregationMethod(AggregationMethod.Minimum);
		} else if (m_maximum.isSelected()) {
			m_settings.aggregationMethod(AggregationMethod.Maximum);
		} else if (m_average.isSelected()) {
			m_settings.aggregationMethod(AggregationMethod.Average);
		}

		m_settings.saveSettingsTo(settings);
	}
}
