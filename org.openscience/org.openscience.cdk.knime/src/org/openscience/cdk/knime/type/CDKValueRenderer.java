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
package org.openscience.cdk.knime.type;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.AROMATICITY;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.layout.LayoutHelper;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.SmartAtomNumberGenerator;
import org.openscience.cdk.renderer.generators.SmartExtendedAtomGenerator;
import org.openscience.cdk.renderer.generators.SmartRingGenerator;
import org.openscience.cdk.renderer.visitor.SmartAWTDrawVisitor;

/**
 * Renderer for {@link CDKValue}s. It will use CDK classes to render a 2D
 * structure of a molecule.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Andreas Truszkowski, EMBL-EBI
 * @author Stephan Beisken, EMBL-EBI
 * @author Mark Reijnberg, EMBL-EBI
 * @author Christoph Steinbeck, EMBL-EBI
 */
public class CDKValueRenderer extends AbstractPainterDataValueRenderer {

	public enum TYPE {
		ALL_ATOMS(""), C_ATOMS("C"), H_ATOMS("H");

		private final String symbol;

		private TYPE(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	};

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKValueRenderer.class);

	private static AtomContainerRenderer renderer;
	private static AtomContainerRenderer RENDERER;

	private static final double SCALE = 0.85;

	static {
		try {
			List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
			generators.add(new BasicSceneGenerator());
			generators.add(new SmartRingGenerator());
			generators.add(new SmartExtendedAtomGenerator());
			generators.add(new SmartAtomNumberGenerator());
			renderer = new AtomContainerRenderer(generators, new AWTFontManager());

			setDefaultRendererProps(renderer.getRenderer2DModel(), CDKNodePlugin.showAomaticity());

		} catch (Exception e) {
			LOGGER.error("Error during renderer initialization!", e);
		}

		RENDERER = renderer;
	}

	private static void setDefaultRendererProps(final RendererModel renderer2dModel, final AROMATICITY aromaticity) {

		if (aromaticity.equals(AROMATICITY.SHOW_KEKULE)) {
			renderer.getRenderer2DModel().set(SmartRingGenerator.ShowAromaticity.class, false);
		} else {
			renderer.getRenderer2DModel().set(SmartRingGenerator.ShowAromaticity.class, true);
		}

		renderer2dModel.set(SmartRingGenerator.MaxDrawableAromaticRing.class, 9);
		renderer2dModel.set(BasicSceneGenerator.UseAntiAliasing.class, true);
		renderer2dModel.set(BasicAtomGenerator.ShowExplicitHydrogens.class, true);
		renderer2dModel.set(BasicAtomGenerator.ShowEndCarbons.class, true);
		renderer2dModel.set(SmartExtendedAtomGenerator.ShowImplicitHydrogens.class, true);
		renderer2dModel.set(SmartAtomNumberGenerator.WillDrawAtomNumbers.class, false);
	}

	private IAtomContainer m_mol;

	private static final Font NO_2D_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

	public CDKValueRenderer() {

		super();

		AROMATICITY aromaticity;

		switch (CDKNodePlugin.showAomaticity()) {

		case SHOW_KEKULE:
			aromaticity = AROMATICITY.SHOW_KEKULE;
			break;
		default:
			aromaticity = AROMATICITY.SHOW_RINGS;
			break;
		}

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
			setRendererProps(TYPE.ALL_ATOMS, numbering, aromaticity);
			break;
		case CARBON:
			setRendererProps(TYPE.C_ATOMS, numbering, aromaticity);
			break;
		case HYDROGEN:
			setRendererProps(TYPE.H_ATOMS, numbering, aromaticity);
			break;
		default:
			setDefaultRendererProps(renderer.getRenderer2DModel(), aromaticity);
			break;
		}
	}

	/**
	 * Sets the numbering parameters for the renderer model.
	 * 
	 * @param type the element symbol to be replaced by a number
	 * @param numbering the numbering scheme (sequential, canonical)
	 */
	private void setRendererProps(final TYPE type, final NUMBERING numbering, final AROMATICITY aromaticity) {

		renderer.getRenderer2DModel().set(SmartExtendedAtomGenerator.ShowImplicitHydrogens.class, false);
		renderer.getRenderer2DModel().set(BasicAtomGenerator.ShowEndCarbons.class, false);
		renderer.getRenderer2DModel().set(SmartAtomNumberGenerator.WillDrawAtomNumbers.class, true);
		renderer.getRenderer2DModel().set(SmartAtomNumberGenerator.DrawSpecificElement.class, type.getSymbol());

		if (aromaticity.equals(AROMATICITY.SHOW_KEKULE)) {
			renderer.getRenderer2DModel().set(SmartRingGenerator.ShowAromaticity.class, false);
		} else {
			renderer.getRenderer2DModel().set(SmartRingGenerator.ShowAromaticity.class, true);
		}

		if (numbering.equals(NUMBERING.SEQUENTIAL)) {
			renderer.getRenderer2DModel().set(SmartAtomNumberGenerator.DrawSequential.class, true);
		} else {
			renderer.getRenderer2DModel().set(SmartAtomNumberGenerator.DrawSequential.class, false);
		}
	}

	/**
	 * Sets a new object to be rendered.
	 * 
	 * @param con the new molecule to be rendered (<code>null</code> is ok)
	 */
	protected void setAtomContainer(final IAtomContainer con) {
		m_mol = con;
		LayoutHelper.adjustStereo(m_mol);
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
	 * @param value the string value for this cell; if value is
	 *        <code>null</code> it sets the text value to an empty string
	 * @see javax.swing.JLabel#setText
	 * 
	 */
	@Override
	protected void setValue(final Object value) {

		if (value instanceof CDKValue) { // when used directly on CDKCell
			setAtomContainer(((CDKValue) value).getAtomContainer());
		} else if ((value instanceof AdapterValue) && ((AdapterValue) value).isAdaptable(CDKValue.class)
				&& (((AdapterValue) value).getAdapterError(CDKValue.class) == null)) {
			setAtomContainer(((AdapterValue) value).getAdapter(CDKValue.class).getAtomContainer());
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

		// if not connected, draw every compound in succession next to each
		// other
		if (!ConnectivityChecker.isConnected(m_mol)) {
			double cumX = 0;
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(m_mol);

			molSet.sortAtomContainers(new Comparator<IAtomContainer>() {

				@Override
				public int compare(IAtomContainer o1, IAtomContainer o2) {

					if (o1.getBondCount() < o2.getBondCount()) {
						return 1;
					} else if (o1.getBondCount() > o2.getBondCount()) {
						return -1;
					}

					return 0;
				}
			});

			Dimension dim = GeometryTools.get2DDimension(molSet.getAtomContainer(0));
			cont.add(molSet.getAtomContainer(0));

			for (int i = 1; i < molSet.getAtomContainerCount(); i++) {
				IAtomContainer curMol = molSet.getAtomContainer(i);
				cumX += dim.width;
				GeometryTools.translate2D(curMol, cumX, 0);
				dim = GeometryTools.get2DDimension(curMol);
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

		RENDERER.paint(cont, new SmartAWTDrawVisitor(g2), new Rectangle(x, y, width, height), true);
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
