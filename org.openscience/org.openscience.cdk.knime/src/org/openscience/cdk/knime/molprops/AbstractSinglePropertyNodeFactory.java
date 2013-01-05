/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.molprops;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public abstract class AbstractSinglePropertyNodeFactory extends NodeFactory<SinglePropertyNodeModel> {

	private final String m_descriptorClassName;

	/**
	 * Creates new factory for a given descriptor class.
	 * 
	 * @param descriptorClassName The class name of the CDK descriptor.
	 */
	public AbstractSinglePropertyNodeFactory(final String descriptorClassName) {

		if (descriptorClassName == null) {
			throw new NullPointerException("Argument must not be null.");
		}
		m_descriptorClassName = descriptorClassName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected NodeDialogPane createNodeDialogPane() {

		return new SinglePropertyNodeDialogPane();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SinglePropertyNodeModel createNodeModel() {

		return new SinglePropertyNodeModel(m_descriptorClassName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<SinglePropertyNodeModel> createNodeView(final int viewIndex, final SinglePropertyNodeModel nodeModel) {

		throw new IndexOutOfBoundsException("No view available: " + viewIndex);
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
