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
package org.openscience.cdk.knime.type;

import java.awt.Color;
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
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.AbstractPainterDataValueRenderer;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.layout.LayoutHelper;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.ModCPKAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

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

	private static final String DESCRIPTION = "CDK Molecule";

	/**
	 * Factory for the {@link CDKValueRenderer}.
	 */
	public static final class Factory extends AbstractDataValueRendererFactory {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDescription() {
			return DESCRIPTION;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
			return new CDKValueRenderer();
		}
	}

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
	
	static {
		try {
			List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
			generators.add(new BasicSceneGenerator());
			generators.add(new StandardGenerator(new Font("Verdana", Font.PLAIN, 18)));
			renderer = new AtomContainerRenderer(generators, new AWTFontManager());

			setDefaultRendererProps(renderer.getRenderer2DModel());

		} catch (Exception e) {
			LOGGER.error("Error during renderer initialization!", e);
		}
	}

	private static void setDefaultRendererProps(final RendererModel renderer2dModel) {

		renderer2dModel.set(BasicSceneGenerator.UseAntiAliasing.class, true);
		renderer2dModel.set(StandardGenerator.AtomColor.class, new ModCPKAtomColors());
		renderer2dModel.set(StandardGenerator.AnnotationColor.class, Color.RED);
		renderer2dModel.set(StandardGenerator.Highlighting.class, StandardGenerator.HighlightStyle.OuterGlow);
		renderer2dModel.set(StandardGenerator.Visibility.class, SymbolVisibility.iupacRecommendations());
	}

	private NUMBERING numbering;
	private IAtomContainer m_mol;

	private static final Font NO_2D_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

	public CDKValueRenderer() {

		super();
		numbering = CDKNodePlugin.numbering();
	}

	/**
	 * Sets a new object to be rendered.
	 *
	 * @param con the new molecule to be rendered (<code>null</code> is ok)
	 */
	protected void setAtomContainer(final IAtomContainer con) {
		m_mol = con;
		if (m_mol != null) {
			try {
				LayoutHelper.adjustStereo(m_mol);
			} catch (IllegalArgumentException exception) {
				m_mol = con;
			}
			
			switch (numbering) {
				case SEQUENTIAL:
					int i = 1;
					for (IAtom atom : con.atoms()) {
					    String label = Integer.toString(i++);
					    atom.setProperty(StandardGenerator.ANNOTATION_LABEL, label);
					}
					break;
				case CANONICAL:
					for (IAtom atom : con.atoms()) {
					    String label = atom.getID();
					    atom.setProperty(StandardGenerator.ANNOTATION_LABEL, label);
					}
					break;
				case NONE:
					break;
			}
		}
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

		boolean twoThreeD = false;
		if (GeometryTools.has2DCoordinates(m_mol) && GeometryTools.has3DCoordinates(m_mol)) {
			g.drawString("Using 2D coordinates", 2, 14);
			twoThreeD = true;
		}

		int x = 0;
		int y = 0;
		int width = getWidth();
		int height = getHeight();

		Graphics2D g2 = (Graphics2D) g;

		if (threeD || twoThreeD) {
			y += 14;
			height -= 14;
		}

		IAtomContainer cont = new AtomContainer();

		// if not connected, draw every compound in succession next to each other
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

		renderer.paint(cont, new AWTDrawVisitor(g2), new Rectangle(x, y, width, height), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDescription() {
		return DESCRIPTION;
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
