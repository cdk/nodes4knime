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
package org.openscience.cdk.knime.commons;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.knime.core.node.NodeLogger;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.Mol2Reader;
import org.openscience.cdk.silent.AtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

public class MolConverter {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(MolConverter.class);

	private interface Conv {

		public IAtomContainer convert(final String notation) throws Exception;
	}

	private static class SdfConv implements Conv {

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			MDLV2000Reader reader = new MDLV2000Reader(new StringReader(notation));
			IAtomContainer molecule = reader.read(new AtomContainer());
			reader.close();

			return molecule;
		}
	}

	private static class Mol2Conv implements Conv {

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			Mol2Reader reader = new Mol2Reader(new StringReader(notation));
			IAtomContainer molecule = reader.read(new AtomContainer());
			reader.close();

			return molecule;
		}
	}

	private static class CMLConv implements Conv {

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			CMLReader reader = new CMLReader(new ByteArrayInputStream(notation.getBytes()));
			IChemFile chemFile = (ChemFile) reader.read(new ChemFile());
			IAtomContainer molecule = ChemFileManipulator.getAllAtomContainers(chemFile).get(0);
			reader.close();

			return molecule;
		}
	}

	private static class SmilesConv implements Conv {

		private final SmilesParser reader = new SmilesParser(SilentChemObjectBuilder.getInstance());

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			return reader.parseSmiles(notation);
		}
	}

	private static class InChIConv implements Conv {

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			final InChIGeneratorFactory inchiFactory = InChIGeneratorFactory.getInstance();
			InChIToStructure gen = inchiFactory.getInChIToStructure(notation, SilentChemObjectBuilder.getInstance());

			return gen.getAtomContainer();
		}
	}

	private static class StringConv implements Conv {

		@Override
		public IAtomContainer convert(final String notation) throws Exception {

			if (notation.startsWith("InChI")) {
				return new InChIConv().convert(notation);
			} else {
				return new SmilesConv().convert(notation);
			}
		}
	}

	public static class Builder {

		private FORMAT format;
		private Conv converter;
		private boolean configure;
		private boolean coordinates;
		private boolean coordinatesForce;

		public Builder(FORMAT format) {

			this.format = format;
			
			switch (format) {
			case SMILES:
				this.converter = new SmilesConv();
				break;
			case CML:
				this.converter = new CMLConv();
				break;
			case INCHI:
				this.converter = new InChIConv();
				break;
			case MOL2:
				this.converter = new Mol2Conv();
				break;
			case SDF:
				this.converter = new SdfConv();
				break;
			case MOL:
				this.converter = new SdfConv();
				break;
			default:
				this.converter = new StringConv();
				break;
			}
			
			this.configure = false;
			this.coordinates = false;
			this.coordinatesForce = false;
		}

		public Builder configure() {
			this.configure = true;
			return this;
		}

		public Builder coordinates() {
			this.coordinates = true;
			return this;
		}

		public Builder coordinates(final boolean force) {
			this.coordinates = true;
			this.coordinatesForce = force;
			return this;
		}

		public MolConverter build() {
			return new MolConverter(this);
		}
	}

	private FORMAT format;
	private Conv converter;
	private boolean configure;
	private boolean coordinates;
	private boolean coordinatesForce;

	public enum FORMAT {
		SMILES, MOL2, INCHI, MOL, SDF, CML, STRING
	}

	private MolConverter(final Builder builder) {

		this.format = builder.format;
		this.converter = builder.converter;
		this.configure = builder.configure;
		this.coordinates = builder.coordinates;
		this.coordinatesForce = builder.coordinatesForce;
	}

	public IAtomContainer convert(final String notation) {

		IAtomContainer mol = null;
		try {
			mol = converter.convert(notation);

			boolean fixBondOrder = false;
			for (IBond bond : mol.bonds()) {
				IBond.Order order = checkNotNull(bond.getOrder(), "Aromaticity model requires that bond orders must be set");
				if (order == Order.UNSET) {
					fixBondOrder = true;
					break;
				}
			}
			
			if (fixBondOrder)
				mol = CDKNodeUtils.fixBondOrder(mol);
			if (configure)
				mol = CDKNodeUtils.getFullMolecule(mol);
			if (coordinates)
				mol = CDKNodeUtils.calculateCoordinates(mol, coordinatesForce);
		} catch (Exception exception) {
			mol = null;
			LOGGER.warn("Format conversion failed.", exception);
		}

		return mol;
	}
	
	public FORMAT format() {
		return format;
	}
}
