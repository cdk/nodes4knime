/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.type;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.type.renderer.ElementNumberGenerator;
import org.openscience.cdk.knime.type.renderer.ElementNumberGenerator.NUMBERING;
import org.openscience.cdk.knime.type.renderer.ElementNumberGenerator.TYPE;
import org.openscience.jchempaint.renderer.AtomContainerRenderer;
import org.openscience.jchempaint.renderer.RendererModel;
import org.openscience.jchempaint.renderer.font.AWTFontManager;
import org.openscience.jchempaint.renderer.generators.BasicAtomGenerator;
import org.openscience.jchempaint.renderer.generators.ExtendedAtomGenerator;
import org.openscience.jchempaint.renderer.generators.RingGenerator;
import org.openscience.jchempaint.renderer.visitor.AWTDrawVisitor;

/**
 * Renderer for {@link CDKValue}s. It will use CDK classes to render a 2D structure of a molecule.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Andreas Truszkowski, EMBL-EBI
 * @author Stephan Beisken, EMBL-EBI
 * @author Mark Reijnberg, EMBL-EBI
 * @author Christoph Steinbeck, EMBL-EBI
 */
public class CDKValueRenderer extends AbstractPainterDataValueRenderer {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKValueRenderer.class);

	private static AtomContainerRenderer renderer;
	private static AtomContainerRenderer rendererNumber;
	
	private static AtomContainerRenderer RENDERER;
	private static final double SCALE = 0.9;

	static {
		try {
			renderer = new AtomContainerRenderer(Arrays.asList(new RingGenerator(), new ExtendedAtomGenerator(),
					new ElementNumberGenerator()), new AWTFontManager(), true);

			RendererModel renderer2dModel = renderer.getRenderer2DModel();
			setRendererProps(renderer2dModel);
			
			rendererNumber = new AtomContainerRenderer(Arrays.asList(new RingGenerator(), new BasicAtomGenerator(),
					new ElementNumberGenerator()), new AWTFontManager(), true);
			
			RendererModel renderer2dModelNumber = rendererNumber.getRenderer2DModel();
			setRendererProps(renderer2dModelNumber);
			renderer2dModelNumber.setDrawNumbers(true);
			
		} catch (Exception e) {
			LOGGER.error("Error during renderer initialization!", e);
		}

		RENDERER = renderer;
	}
	
	private static void setRendererProps(RendererModel renderer2dModel) {
		
		renderer2dModel.setUseAntiAliasing(true);
		renderer2dModel.setShowAtomAtomMapping(false);
		renderer2dModel.setShowAtomTypeNames(false);
		renderer2dModel.setShowExplicitHydrogens(true);
		renderer2dModel.setShowAromaticity(true);
	}

	private IAtomContainer m_mol;

	private static final Font NO_2D_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

	public CDKValueRenderer() {

		super();

		NUMBERING numbering;

		switch (CDKNodePlugin.numbering()) {

		case SEQUENTIAL:
			numbering = NUMBERING.SEQUENTIAL;
			break;
		default:
			numbering = NUMBERING.CANONICAL;
			break;
		}

		switch (CDKNodePlugin.showNumbers()) {

		case ALL:
			setNumberRenderer(TYPE.ALL_ATOMS, numbering);
			break;
		case CARBON:
			setNumberRenderer(TYPE.C_ATOMS, numbering);
			break;
		case HYDROGEN:
			setNumberRenderer(TYPE.H_ATOMS, numbering);
			break;
		default:
			RENDERER = renderer;
			break;
		}
	}

	private void setNumberRenderer(TYPE type, NUMBERING numbering) {

		RENDERER = rendererNumber;
		((ElementNumberGenerator) RENDERER.getGenerators().get(2)).setType(type, numbering);
	}

	/**
	 * Sets a new object to be rendered.
	 * 
	 * @param con the new molecule to be rendered (<code>null</code> is ok)
	 */
	protected void setAtomContainer(final IAtomContainer con) {

		m_mol = con;
	}

	/**
	 * Get the currently set molecule for rendering (may be <code>null</code>).
	 * 
	 * @return the current molecule
	 */
	protected IAtomContainer getAtomContainer() {

		return m_mol;
	}

	/**
	 * Sets the string object for the cell being rendered.
	 * 
	 * @param value the string value for this cell; if value is <code>null</code> it sets the text value to an empty
	 *        string
	 * @see javax.swing.JLabel#setText
	 * 
	 */
	@Override
	protected void setValue(final Object value) {

		if (value instanceof CDKValue) { // when used directly on CDKCell
			setAtomContainer(((CDKValue) value).getAtomContainer());
			return;
		} else {
			setAtomContainer(null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void paintComponent(final Graphics g) {

		super.paintComponent(g);
		g.setFont(NO_2D_FONT);

		if (m_mol == null) {
			g.drawString("Object missing ('?')", 2, 14);
			return;
		}

		boolean threeD = false;
		if (!GeometryTools.has2DCoordinates(m_mol)) {
			if (GeometryTools.has3DCoordinates(m_mol)) {
				g.drawString("3D view not supported", 2, 14);
				threeD = true;
			} else {
				g.drawString("No 2D coordinates", 2, 14);
				return;
			}

		}

		if (GeometryTools.has2DCoordinates(m_mol) && GeometryTools.has3DCoordinates(m_mol)) {
			g.drawString("3D view not supported", 2, 14);
			threeD = true;
		}

		int x = 0;
		int y = 0;
		int width = getWidth();
		int height = getHeight();

		Graphics2D g2 = (Graphics2D) g;

		if (SCALE < 1.0) {
			x = (int) ((width * (1.0 - SCALE)) / 2);
			y = (int) ((height * (1.0 - SCALE)) / 2);
			width = (int) (width * SCALE);
			height = (int) (height * SCALE);
		}

		if (threeD) {
			y += 14;
			height -= 14;
		}

		IAtomContainer cont = new AtomContainer();
		Dimension aPrefferedSize = new Dimension(width, height);

		// if not connected, draw every compound in succession next to each other
		if (!ConnectivityChecker.isConnected(m_mol)) {
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(m_mol);
			Rectangle2D molRec = GeometryTools.getRectangle2D(molSet.getAtomContainer(0));
			cont.add(molSet.getAtomContainer(0));

			for (int i = 1; i < molSet.getAtomContainerCount(); i++) {
				IAtomContainer curMol = molSet.getAtomContainer(i);
				Rectangle2D molRecCur = GeometryTools.getRectangle2D(curMol);
				double xShift = molRec.getCenterX() + (molRec.getWidth() / 2) + (molRecCur.getWidth() / 2);
				double yShift = molRecCur.getCenterY();
				GeometryTools.translate2DCenterTo(curMol, new Point2d(new double[] { xShift, yShift }));

				molRec = molRecCur;
				cont.add(curMol);
			}
		} else {
			cont = m_mol;
		}

		// flatten 3D mol
		if (threeD) {
			Point3d p3d;
			for (IAtom atom : cont.atoms()) {
				p3d = atom.getPoint3d();
				atom.setPoint2d(new Point2d(p3d.x, p3d.y));
			}
		}

		GeometryTools.translateAllPositive(cont);
		GeometryTools.scaleMolecule(cont, aPrefferedSize, 0.8f);
		GeometryTools.center(cont, aPrefferedSize);
		
		RENDERER.paintMolecule(cont, new AWTDrawVisitor(g2), new Rectangle(x, y, width, height), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {

		return "CDK Molecule";
	}

	/**
	 * @return new pref dimension object
	 * @see javax.swing.JComponent#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {

		return new Dimension(100, 100);
	}
}
