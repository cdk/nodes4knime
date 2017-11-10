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
package org.openscience.cdk.knime.nodes.smarts;

import javax.swing.JCheckBox;

import org.knime.chem.types.SmartsValue;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKDialog;

/**
 * This factory creates the model for the SMARTS node.
 * 
 * @author Stephan Beisken, EMBL-EBI
 */
public class SmartsNodeFactory extends NodeFactory<SmartsNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {
		
		CDKDialog dialog = new CDKDialog();
		dialog.addColumnSelection("Molecule", CDKNodeUtils.ACCEPTED_VALUE_CLASSES);
		dialog.addColumnSelection("SMARTS", 1, SmartsValue.class);
		dialog.addCustomOption("Count Unique", new JCheckBox("", false));
		dialog.addCustomOption("Record match positions", new JCheckBox("", false));
		return dialog.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SmartsNodeModel createNodeModel() {
		return new SmartsNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<SmartsNodeModel> createNodeView(final int viewIndex, final SmartsNodeModel nodeModel) {
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
