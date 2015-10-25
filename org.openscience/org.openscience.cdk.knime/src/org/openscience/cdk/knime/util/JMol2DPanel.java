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
package org.openscience.cdk.knime.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.swing.JPanel;

import org.knime.core.node.NodeLogger;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.jchempaint.renderer.AtomContainerRenderer;
import org.openscience.jchempaint.renderer.font.AWTFontManager;
import org.openscience.jchempaint.renderer.generators.BasicAtomGenerator;
import org.openscience.jchempaint.renderer.generators.BasicBondGenerator;
import org.openscience.jchempaint.renderer.visitor.AWTDrawVisitor;

/**
 * This panel shows 2D structure diagrams of CDK molecules.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class JMol2DPanel extends JPanel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(JMol2DPanel.class);

	private final AtomContainerRenderer m_renderer;

	private IAtomContainer m_mol;

	/**
	 * Instantiates new renderer.
	 */
	public JMol2DPanel() {

		m_renderer = new AtomContainerRenderer(Arrays.asList(new BasicBondGenerator(), new BasicAtomGenerator()),
				new AWTFontManager(), false);
	}

	/**
	 * Sets a new object to be rendered.
	 * 
	 * @param con the new molecule to be rendered (<code>null</code> is ok)
	 */
	public void setAtomContainer(final IAtomContainer con) {

		m_mol = con;
	}

	/**
	 * Get the currently set molecule for rendering (may be <code>null</code>).
	 * 
	 * @return the current molecule
	 */
	public IAtomContainer getAtomContainer() {

		return m_mol;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		super.paintComponent(g);
		try {
			Dimension d = getSize();

			// layout is handled by the paint method
			Rectangle2D drawArea = new Rectangle2D.Double(0, 0, d.getWidth(), d.getHeight());
			m_renderer.paintMolecule(m_mol, new AWTDrawVisitor((Graphics2D) g), drawArea, true);
		} catch (Exception e) {
			LOGGER.debug("Unable to paint molecule \"" + m_mol.toString() + "\": " + e.getMessage(), e);
		}
	}
}
