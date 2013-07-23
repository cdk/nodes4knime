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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.jchempaint.JChemPaintPanel;

/**
 * This is a panel that lets the user draw structures and returns them as SDF string. They can be loaded again into an
 * empty panel afterwards.
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

	/**
	 * Returns a SDF string for the drawn molecules.
	 * 
	 * @return a SDF string
	 */
	public String getSDF() {

		IAtomContainerSet s = getChemModel().getMoleculeSet();
		SDFWriter sdfWriter = null;
		StringWriter stringWriter = null;

		try {
			stringWriter = new StringWriter();
			sdfWriter = new SDFWriter(stringWriter);

			sdfWriter.write(s);

		} catch (CDKException exception) {
			// do nothing
		} finally {
			try {
				sdfWriter.close();
				stringWriter.close();
			} catch (IOException exception) {
				// do nothing
			}
		}

		return stringWriter.toString();
	}

	public static IAtomContainerSet readStringNotation(String stringNotation) throws CDKException {

		AtomContainerSet atomContainerSet = new AtomContainerSet();

		BufferedReader stringReader = new BufferedReader(new StringReader(stringNotation));
		try {
			if (stringReader.readLine().length() > 80 || stringReader.readLine().length() > 80
					|| stringReader.readLine().length() > 80 || !stringReader.readLine().contains("V2000")) {

				CMLReader reader = new CMLReader(new ByteArrayInputStream(stringNotation.getBytes()));
				IChemFile chemFile = (ChemFile) reader.read(new ChemFile());
				for (IAtomContainer container : ChemFileManipulator.getAllAtomContainers(chemFile)) {
					atomContainerSet.addAtomContainer(container);
				}
				reader.close();

			} else {

				IteratingSDFReader reader = new IteratingSDFReader(new StringReader(stringNotation),
						SilentChemObjectBuilder.getInstance());

				while (reader.hasNext()) {
					IAtomContainer cdkMol = reader.next();
					cdkMol.removeProperty("cdk:Title");

					if (cdkMol != null) {
						CDKNodeUtils.getStandardMolecule(cdkMol);
					}
					for (IAtom atom : cdkMol.atoms()) {
						atom.setValency(null);
					}
					atomContainerSet.addAtomContainer(cdkMol);
					reader.close();
				}
			}

			stringReader.close();
		} catch (Exception exception) {
			throw new CDKException(exception.getMessage());
		}
		
		return atomContainerSet;
	}
}
