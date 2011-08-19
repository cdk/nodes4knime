/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   31.08.2007 (thor): created
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
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(JMol2DPanel.class);

    private final AtomContainerRenderer m_renderer;

    private IAtomContainer m_mol;

    /**
     * Instantiates new renderer.
     */
	public JMol2DPanel() {
        m_renderer = new AtomContainerRenderer(Arrays.asList(
        		new BasicBondGenerator(), new BasicAtomGenerator()), 
        		new AWTFontManager(), false);
        
        // anti aliasing, true
        // show aromaticity, true
        // background color white
        // show explicit hydrogens, true
        // additional parameters must extend 'IGeneratorParameter'
        // and implemented manually once needed,
        // parameters can be added via the 'RendererModel'
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
            
//            if (m_mol != null && GeometryTools.has2DCoordinates(m_mol)) {
//                GeometryTools.translateAllPositive(m_mol, m);
//                double scale = GeometryTools.getScaleFactor(m_mol, 30.0, m);
//                GeometryTools.scaleMolecule(m_mol, scale, m);
//                Dimension prefDim = GeometryTools.get2DDimension(m_mol, m);
//                if ((prefDim.getWidth() > 0.8 * d.getWidth())
//                        || (prefDim.getHeight() > 0.8 * d.getHeight())) {
//                    GeometryTools.scaleMolecule(m_mol, d, 0.8, m);
//                }
//                GeometryTools.center(m_mol, d, m);
//                m_renderer.paintMolecule(m_mol, (Graphics2D)g, false, true);
//            }
            // layout is handled by the paint method
            Rectangle2D drawArea = new Rectangle2D.Double(0, 0, d.getWidth(), d.getHeight());
            m_renderer.paintMolecule(m_mol, new AWTDrawVisitor((Graphics2D) g), drawArea, true);
        } catch (Exception e) {
            LOGGER.debug("Unable to paint molecule \"" + m_mol.toString()
                    + "\": " + e.getMessage(), e);
        }
    }
}
