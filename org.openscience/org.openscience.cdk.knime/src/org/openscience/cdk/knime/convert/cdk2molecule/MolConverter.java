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
package org.openscience.cdk.knime.convert.cdk2molecule;

import java.io.StringWriter;
import java.util.Properties;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.chem.types.CMLCell;
import org.knime.chem.types.CMLCellFactory;
import org.knime.chem.types.Mol2Cell;
import org.knime.chem.types.Mol2CellFactory;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.io.listener.PropertiesListener;
import org.openscience.cdk.knime.convert.cdk2molecule.CDK2MoleculeSettings.Format;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Helper class for converting CDK molecules into strings representations.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
class MolConverter implements ExtendedCellFactory {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDK2MoleculeNodeModel.class);

	private interface Conv {

		/**
		 * Converts the CDK molecules and returns a data cell.
		 * 
		 * @param mol the CDK molecule
		 * @return a data cell with the string representation
		 * @throws Exception if an exception occurs
		 */
		DataCell conv(IAtomContainer mol) throws Exception;
	}

	private class SdfConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataCell conv(final IAtomContainer mol) throws Exception {

			// removes configuration and valence annotation
			StringWriter out = new StringWriter(1024);
			MDLV2000Writer writer = new MDLV2000Writer(out);
			writer.writeMolecule(mol);
			writer.close();
			out.append("$$$$");
			return SdfCellFactory.create(out.toString());
		}
	}

	private class Mol2Conv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataCell conv(final IAtomContainer mol) throws Exception {

			StringWriter out = new StringWriter(1024);
			Mol2Writer writer = new Mol2Writer(out);
			writer.writeMolecule(mol);
			writer.close();
			return Mol2CellFactory.create(out.toString());
		}
	}

	private class SmilesConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataCell conv(final IAtomContainer mol) throws Exception {

			StringWriter out = new StringWriter(1024);
			SMILESWriter writer = new SMILESWriter(out);
			Properties prop = new Properties();
	        prop.setProperty("UseAromaticity","true");
	        PropertiesListener listener = new PropertiesListener(prop);
	        writer.addChemObjectIOListener(listener);
	        writer.customizeJob();
			writer.writeAtomContainer(mol);
			writer.close();
			
			String smiles = out.toString().trim();
			if (smiles == null || smiles.isEmpty()) {
				throw new CDKException("Smiles generation failed.");
			}
			
			return new SmilesCell(out.toString().trim());
		}
	}

	private class CMLConv implements Conv {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public DataCell conv(final IAtomContainer mol) throws Exception {

			StringWriter out = new StringWriter(1024);
			CMLWriter writer = new CMLWriter(out);
			writer.write(mol);
			writer.close();
			return CMLCellFactory.create(out.toString());
		}
	}

	private final Conv m_converter;
	private final ColumnDestination[] m_colDest;
	private final DataColumnSpec[] m_colSpec;
	private final int m_colIndex;

	/**
	 * Creates a new converter.
	 * 
	 * @param inSpec the spec of the input table
	 * @param settings the settings of the converter node
	 * @param pool the thread pool that should be used for converting
	 */
	public MolConverter(final DataTableSpec inSpec, final CDK2MoleculeSettings settings) {

		DataType type = null;
		if (settings.destFormat() == Format.SDF) {
			type = SdfCell.TYPE;
			m_converter = new SdfConv();
		} else if (settings.destFormat() == Format.Smiles) {
			type = SmilesCell.TYPE;
			m_converter = new SmilesConv();
		} else if (settings.destFormat() == Format.Mol2) {
			type = Mol2Cell.TYPE;
			m_converter = new Mol2Conv();
		} else {
			type = CMLCell.TYPE;
			m_converter = new CMLConv();
		}
		
		m_colIndex = inSpec.findColumnIndex(settings.columnName());
		if (settings.replaceColumn()) {
			m_colSpec = new DataColumnSpec[] { new DataColumnSpecCreator(settings.columnName(), type)
					.createSpec() };
			m_colDest = new ColumnDestination[] { new ReplaceColumn(m_colIndex) };
		} else {
			m_colSpec = new DataColumnSpec[] { new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpec,
					settings.newColumnName()), type).createSpec() };
			m_colDest = new ColumnDestination[] { new AppendColumn() };
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(final DataRow row) {

		final DataCell cell = row.getCell(m_colIndex);

		if (cell.isMissing()) {
			return new DataCell[] { DataType.getMissingCell() };
		}
		
		DataCell retCell;
		try {
			retCell = m_converter.conv(((CDKValue) cell).getAtomContainer());
		} catch (Exception ex) {
			LOGGER.error("Could not convert molecules: " + ex.getMessage(), ex);
			retCell = DataType.getMissingCell();
		}
		return new DataCell[] { retCell };
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
