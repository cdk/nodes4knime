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
package org.openscience.cdk.knime.view3d;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKDialog;

/**
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerNodeFactory extends NodeFactory<JmolViewerNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JmolViewerNodeModel createNodeModel() {
		return new JmolViewerNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getNrNodeViews() {
		return 1;
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
		dialog.addColumnSelection("Molecule", CDKNodeUtils.ACCEPTED_VALUE_CLASSES);
		
		return dialog.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<JmolViewerNodeModel> createNodeView(final int viewIndex, final JmolViewerNodeModel nodeModel) {

		if (viewIndex != 0) {
			throw new IndexOutOfBoundsException("Invalid index: " + viewIndex);
		}
		return new JmolViewerNodeView(nodeModel);
	}

}
