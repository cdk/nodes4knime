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
package org.openscience.cdk.knime.convert.molecule2cdk;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.chem.types.CMLValue;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.Mol2Reader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.FixBondOrdersTool;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Helper class for converting string representations into CDK molecules.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
class MolConverter implements ExtendedCellFactory {

	private static final CDKHydrogenAdder hydra = CDKHydrogenAdder.getInstance(SilentChemObjectBuilder.getInstance());

	private interface Conv {

		/**
		 * Converts a molecule's string representation into a CDK object.
		 * 
		 * @param cell a data cell with a molecule string
		 * @return a CDK molecule
		 * @throws CDKException if an error occurs during conversion
		 */
		public IAtomContainer conv(DataCell cell) throws Exception;
	}

	private class SdfConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception, NumberFormatException {

			String sdf = ((SdfValue) cell).getSdfValue();

			IteratingSDFReader reader = new IteratingSDFReader(new StringReader(sdf),
					SilentChemObjectBuilder.getInstance());
			return reader.next();
		}
	}

	private class MolConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception {

			String mol = ((MolValue) cell).getMolValue();

			MDLV2000Reader reader = new MDLV2000Reader(new StringReader(mol));
			return reader.read(SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class));
		}
	}

	private class Mol2Conv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception {

			String mol2 = ((Mol2Value) cell).getMol2Value();

			Mol2Reader reader = new Mol2Reader(new StringReader(mol2));
			return reader.read(SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class));
		}
	}

	private class CMLConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception {

			String cml = ((CMLValue) cell).getCMLValue();

			CMLReader reader = new CMLReader(new ByteArrayInputStream(cml.getBytes()));
			return reader.read(SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class));
		}
	}

	private class SmilesConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception {

			final String smiles = ((SmilesValue) cell).getSmilesValue();

			final SmilesParser reader = new SmilesParser(SilentChemObjectBuilder.getInstance());
			IAtomContainer cdkMol = reader.parseSmiles(smiles);
			// CMLWriter crashes if chiral centers are not eradicated
			cdkMol = SMSDNormalizer.convertExplicitToImplicitHydrogens(cdkMol);
			return cdkMol;
		}
	}

	private class InChIConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public IAtomContainer conv(final DataCell cell) throws Exception {

			final String inchi = ((StringValue) cell).getStringValue();

			final InChIGeneratorFactory inchiFactory = InChIGeneratorFactory.getInstance();
			InChIToStructure gen = inchiFactory.getInChIToStructure(inchi, SilentChemObjectBuilder.getInstance());

			return gen.getAtomContainer();
		}
	}

	private final ColumnDestination[] m_colDest;
	private final DataColumnSpec[] m_colSpec;
	private final Molecule2CDKSettings m_settings;
	private final int m_colIndex;
	private final Conv m_converter;
	private final FixBondOrdersTool bondDeducer;
	private final ExecutorService executor;

	/**
	 * Creates a new converter.
	 * 
	 * @param inSpec the spec of the input table
	 * @param settings the settings of the converter node
	 * @param pool the thread pool that should be used for converting
	 */
	public MolConverter(final DataTableSpec inSpec, final Molecule2CDKSettings settings, final ExecutorService executor) {

		m_colIndex = inSpec.findColumnIndex(settings.columnName());
		if (settings.replaceColumn()) {
			m_colSpec = new DataColumnSpec[] { new DataColumnSpecCreator(settings.columnName(), CDKCell.TYPE)
					.createSpec() };
			m_colDest = new ColumnDestination[] { new ReplaceColumn(m_colIndex) };
		} else {
			m_colSpec = new DataColumnSpec[] { new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec,
					settings.newColumnName()), CDKCell.TYPE).createSpec() };
			m_colDest = new ColumnDestination[] { new AppendColumn() };
		}

		DataColumnSpec cs = inSpec.getColumnSpec(m_colIndex);
		if (cs.getType().isCompatible(SdfValue.class)) {
			m_converter = new SdfConv();
		} else if (cs.getType().isCompatible(MolValue.class)) {
			m_converter = new MolConv();
		} else if (cs.getType().isCompatible(Mol2Value.class)) {
			m_converter = new Mol2Conv();
		} else if (cs.getType().isCompatible(CMLValue.class)) {
			m_converter = new CMLConv();
		} else if (cs.getType().isCompatible(SmilesValue.class)) {
			m_converter = new SmilesConv();
		} else {
			m_converter = new InChIConv();
		}

		bondDeducer = new FixBondOrdersTool();

		m_settings = settings;
		this.executor = executor;
	}

	@Override
	public DataCell[] getCells(final DataRow row) {

		final DataCell cell = row.getCell(m_colIndex);

		if (cell.isMissing()) {
			return new DataCell[] { DataType.getMissingCell() };
		}

		Callable<IAtomContainer> r = new Callable<IAtomContainer>() {

			public IAtomContainer call() {

				IAtomContainer mol;
				try {
					mol = getAtomContainer(cell);
				} catch (RuntimeException e) {
					mol = null;
				} catch (Exception e) {
					mol = null;
				}
				return mol;
			}
		};

		Future<IAtomContainer> future = (Future<IAtomContainer>) executor.submit(r);
		IAtomContainer molP = null;
		try {
			molP = future.get(m_settings.timeout(), TimeUnit.MILLISECONDS);
			if (molP == null)
				throw new CDKException("Error retrieving molecule.");
			CDKNodeUtils.calculateSmiles(molP);
		} catch (Exception e) {
			return new DataCell[] { DataType.getMissingCell() };
		}

		// remove JCP valency labels
		for (IAtom atom : molP.atoms()) {
			atom.setValency(null);
		}
		
		return new DataCell[] { new CDKCell(molP) };
	}

	private IAtomContainer getAtomContainer(final DataCell cell) throws RuntimeException, Exception {

		IAtomContainer cdkMol = m_converter.conv(cell);

		if (m_settings.convertOrder()) {
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(cdkMol);
			cdkMol = bondDeducer.kekuliseAromaticRings(cdkMol);
			hydra.addImplicitHydrogens(cdkMol);
			CDKHueckelAromaticityDetector.detectAromaticity(cdkMol);
		} else {
			CDKNodeUtils.getStandardMolecule(cdkMol);
		}

		if (m_settings.generate2D())
			cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, m_settings.force2D());

		return cdkMol;
	}

	@Override
	public ColumnDestination[] getColumnDestinations() {

		return m_colDest;
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return m_colSpec;
	}
}
