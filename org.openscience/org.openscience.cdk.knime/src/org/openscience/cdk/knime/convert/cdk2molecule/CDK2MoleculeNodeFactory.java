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
package org.openscience.cdk.knime.convert.cdk2molecule;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * This class is the factory for the CDK->Molecule converter node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDK2MoleculeNodeFactory extends NodeFactory<CDK2MoleculeNodeModel> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {

		return new CDK2MoleculeNodeDialog();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CDK2MoleculeNodeModel createNodeModel() {

		return new CDK2MoleculeNodeModel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<CDK2MoleculeNodeModel> createNodeView(final int index, final CDK2MoleculeNodeModel model) {

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
