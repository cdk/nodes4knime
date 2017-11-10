/*
 * Copyright (c) 2014, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package ambit2.knime.tautomers;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKDialog;

/**
 * Factory method for AMBIT's tautomer generator node.
 * 
 * @author Stephan Beisken, EMBL-EBI
 */
public class TautomerFactory extends NodeFactory<TautomerModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {

		CDKDialog dialog = new CDKDialog();
		dialog.addColumnSelection("Molecule", CDKNodeUtils.ACCEPTED_VALUE_CLASSES);

		JRadioButton allButton = new JRadioButton("all tautomers");
		JRadioButton bestAppendButton = new JRadioButton("best tautomer (append)");
		JRadioButton bestReplaceButton = new JRadioButton("best tautomer (replace)", true);
		ButtonGroup bg = new ButtonGroup();
		bg.add(allButton);
		bg.add(bestAppendButton);
		bg.add(bestReplaceButton);
		dialog.addRadioButtonOption("Generate", allButton, bestAppendButton, bestReplaceButton);

		return dialog.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TautomerModel createNodeModel() {
		return new TautomerModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<TautomerModel> createNodeView(final int viewIndex, final TautomerModel nodeModel) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getNrNodeViews() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean hasDialog() {
		return true;
	}
}
