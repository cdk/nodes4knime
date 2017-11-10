/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.masscalculator;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.knime.core.data.StringValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.openscience.cdk.knime.core.CDKDialog;
import org.openscience.cdk.knime.nodes.masscalculator.MassCalculatorNodeModel.Setting;

/**
 * <code>NodeFactory</code> for the "MassCalculator" Node. This node calculates the molecular weight or molar mass of a
 * sum formula.
 * 
 * @author Stephan Beisken
 */
public class MassCalculatorNodeFactory extends NodeFactory<MassCalculatorNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public MassCalculatorNodeModel createNodeModel() {
		return new MassCalculatorNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<MassCalculatorNodeModel> createNodeView(final int viewIndex, final MassCalculatorNodeModel nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasDialog() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeDialogPane createNodeDialogPane() {
		
		CDKDialog dialog = new CDKDialog();
		
		dialog.addColumnSelection(Setting.COLUMN_NAME.label(), StringValue.class);
		
		JRadioButton molecularMass = new JRadioButton("Molecular Mass", true);
		JRadioButton molarMass = new JRadioButton("Molar Mass", false);
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(molecularMass);
		bg.add(molarMass);
		
		dialog.addRadioButtonOption(Setting.MASS.label(), molecularMass, molarMass);
		
		return dialog.build();
	}
}
