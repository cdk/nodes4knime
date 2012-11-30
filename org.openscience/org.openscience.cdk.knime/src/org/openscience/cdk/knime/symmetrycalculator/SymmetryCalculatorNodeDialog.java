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

package org.openscience.cdk.knime.symmetrycalculator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * <code>NodeDialog</code> for the "SymmetryCalculator" Node.
 * 
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class SymmetryCalculatorNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

	private final JCheckBox visual = new JCheckBox();

	/**
	 * New pane for configuring SymmetryCalculator node dialog. This is just a suggestion to demonstrate possible
	 * default dialog components.
	 */
	protected SymmetryCalculatorNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		p.add(new JLabel("Column with molecules   "), c);
		c.gridx++;
		p.add(molColumn, c);

		c.gridx = 0;
		c.gridy++;
		p.add(new JLabel("Visualisation only   "), c);
		c.gridx++;
		p.add(visual, c);

		addTab("Default settings", p);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		String colName = null;
		try {
			colName = settings.getString(SymmetryCalculatorNodeModel.CFG_COLNAME);
		} catch (InvalidSettingsException ex) {
			// ignore it
		}

		molColumn.update(specs[0], colName);
		visual.setSelected(settings.getBoolean(SymmetryCalculatorNodeModel.VISUAL, false));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		settings.addString(SymmetryCalculatorNodeModel.CFG_COLNAME, molColumn.getSelectedColumn());
		settings.addBoolean(SymmetryCalculatorNodeModel.VISUAL, visual.isSelected());

	}
}
