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
package org.openscience.cdk.knime.util;

import java.awt.Dimension;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.silent.AtomContainerSet;
import org.openscience.jchempaint.JChemPaintPanel;

/**
 * This is a panel that lets the user draw structures and returns them as SDF
 * string. They can be loaded again into an empty panel afterwards.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class JMolSketcherPanel extends JChemPaintPanel {

	/**
	 * Creates a new sketcher panel.
	 */
	public JMolSketcherPanel() {

		super(getModel());
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
	 * @param stringNotation a molecule string
	 * @throws Exception if an exception occurs
	 */
	public void loadStructures(String stringNotation) throws Exception {

		IChemModel chemModel = getChemModel();
		chemModel.setMoleculeSet(JMolSketcherPanel.readStringNotation(stringNotation));
	}

	public static IAtomContainerSet readStringNotation(String stringNotation) throws CDKException {

		IAtomContainer mol = CDKNodeUtils.getFullMolecule(stringNotation);
		mol = CDKNodeUtils.calculateCoordinates(mol, false);
		for (IAtom atom : mol.atoms()) {
			atom.setValency(null);
		}

		IAtomContainerSet atomContainerSet = ConnectivityChecker.partitionIntoMolecules(mol);

		if (atomContainerSet.getAtomContainerCount() != 1) {
			double cumX = 0;
			IAtomContainerSet atomContainerSet2 = new AtomContainerSet();
			Dimension dim = GeometryTools.get2DDimension(atomContainerSet.getAtomContainer(0));
			atomContainerSet2.addAtomContainer(atomContainerSet.getAtomContainer(0));

			for (int i = 1; i < atomContainerSet.getAtomContainerCount(); i++) {
				IAtomContainer curMol = atomContainerSet.getAtomContainer(i);
				cumX += dim.width;
				GeometryTools.translate2D(curMol, cumX, 0);
				dim = GeometryTools.get2DDimension(curMol);
				atomContainerSet2.addAtomContainer(curMol);
			}
			return atomContainerSet2;
		}

		return atomContainerSet;
	}
}
