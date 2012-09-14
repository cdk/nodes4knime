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
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.ReturnType;

/**
 * <code>NodeDialog</code> for the "Similarity" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
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

	private final JRadioButton returnString = new JRadioButton("String");
	private final JRadioButton returnCollection = new JRadioButton("Collection");

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

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Return type   "), c);
		c.gridx = 1;
		p.add(returnString, c);
		c.gridy++;
		p.add(returnCollection, c);
		returnString.setSelected(true);

		ButtonGroup bg1 = new ButtonGroup();
		bg1.add(m_minimum);
		bg1.add(m_maximum);
		bg1.add(m_average);

		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(returnString);
		bg2.add(returnCollection);

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

		if (m_settings.returnType().equals(ReturnType.String)) {
			returnString.setSelected(true);
		} else if (m_settings.returnType().equals(ReturnType.Collection)) {
			returnCollection.setSelected(true);
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
		if (returnString.isSelected()) {
			m_settings.returnType(ReturnType.String);
		} else if (returnCollection.isSelected()) {
			m_settings.returnType(ReturnType.Collection);
		}

		m_settings.saveSettingsTo(settings);
	}
}
