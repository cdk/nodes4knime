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
package org.openscience.cdk.knime.util;

import java.awt.geom.Rectangle2D;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.layout.TemplateHandler;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.jchempaint.JChemPaintPanel;

/**
 * This is a panel that lets the user draw structures and returns them as Smiles strings. They can be loaded again into
 * an empty panel afterwards.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class JMolSketcherPanel extends JChemPaintPanel {

	/**
	 * Creates a new sketcher panel.
	 */
	public JMolSketcherPanel() {

		super(getModel());

		setShowMenuBar(false);
		setIsNewChemModel(true);
	}

	private static IChemModel getModel() {

		IChemModel chemModel = DefaultChemObjectBuilder.getInstance().newInstance(IChemModel.class);
		chemModel.setMoleculeSet(chemModel.getBuilder().newInstance(IAtomContainerSet.class));
		chemModel.getMoleculeSet().addAtomContainer(chemModel.getBuilder().newInstance(IAtomContainer.class));
		
		return chemModel;
	}

	/**
	 * Loads the given structures into the panel.
	 * 
	 * @param smiles a list of Smiles strings
	 * @throws Exception if an exception occurs
	 */
	public void loadStructures(final String... smiles) throws Exception {

		IChemModel chemModel = getChemModel();
		IAtomContainerSet moleculeSet = new AtomContainerSet();
		Rectangle2D molRectangle = null;
		Rectangle2D tmpRectangle = null;
		if (smiles != null && smiles.length > 0) {
			for (int i = 0; i < smiles.length; i++) {
				SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
				IAtomContainer m = parser.parseSmiles(smiles[i]);
				StructureDiagramGenerator sdg = new StructureDiagramGenerator();
				sdg.setTemplateHandler(new TemplateHandler(moleculeSet.getBuilder()));
				sdg.setMolecule(m);
				sdg.generateCoordinates(new Vector2d(0, 1));
				m = sdg.getMolecule();
				// arrange molecules relative to each other
				if (molRectangle == null) {
					molRectangle = GeometryTools.getRectangle2D(m);
				} else {
					tmpRectangle = GeometryTools.getRectangle2D(m);
					double xShift = molRectangle.getCenterX() + (molRectangle.getWidth() / 1.95)
							+ (tmpRectangle.getWidth() / 1.95);
					double yShift = tmpRectangle.getCenterY();
					GeometryTools.translate2DCenterTo(m, new Point2d(new double[] { xShift, yShift }));
					molRectangle = tmpRectangle;
				}
				moleculeSet.addAtomContainer(m);
				// if there are no atoms in the actual chemModel
				// all 2D-coordinates would be set to NaN
				if (ChemModelManipulator.getAtomCount(chemModel) != 0) {
					IAtomContainer cont = chemModel.getBuilder().newInstance(IAtomContainer.class);
					for (Object ac : ChemModelManipulator.getAllAtomContainers(chemModel)) {
						cont.add((IAtomContainer) ac);
					}
				}
				chemModel.setMoleculeSet(moleculeSet);
			}
		}
	}

	/**
	 * Returns an array of Smiles strings for the drawn molecules.
	 * 
	 * @return an array of Smiles strings
	 */
	public String[] getAllSmiles() {

		IAtomContainerSet s = getChemModel().getMoleculeSet();
		ArrayList<String> smiles = new ArrayList<String>();

		for (int i = 0; i < s.getAtomContainerCount(); i++) {

			StringWriter writer = new StringWriter();
			SMILESWriter w = new SMILESWriter(writer);
			w.writeAtomContainer(s.getAtomContainer(i));
			String sm = writer.toString().trim();

			if (sm.length() > 0) {
				smiles.add(sm);
			}
		}
		return smiles.toArray(new String[smiles.size()]);
	}
}
