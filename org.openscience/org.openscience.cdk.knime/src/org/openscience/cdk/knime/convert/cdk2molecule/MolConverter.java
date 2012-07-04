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
package org.openscience.cdk.knime.convert.cdk2molecule;

import java.io.StringWriter;

import org.knime.chem.types.CMLCell;
import org.knime.chem.types.CMLCellFactory;
import org.knime.chem.types.Mol2Cell;
import org.knime.chem.types.Mol2CellFactory;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SdfCellFactory;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Helper class for converting CDK molecules into strings representations.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
class MolConverter extends SingleCellFactory {

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
			IAtomContainer molClone = (IAtomContainer) mol.clone();
			// AtomContainerManipulator.clearAtomConfigurations(molClone);

			StringWriter out = new StringWriter(1024);
			MDLV2000Writer writer = new MDLV2000Writer(out);
			writer.writeMolecule(molClone);
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
			writer.writeAtomContainer(mol);
			writer.close();
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

	private final int m_colIndex;

	MolConverter(final DataColumnSpec cs, final int colIndex) {

		super(cs);
		m_colIndex = colIndex;

		if (cs.getType().equals(SdfCell.TYPE)) {
			m_converter = new SdfConv();
		} else if (cs.getType().equals(Mol2Cell.TYPE)) {
			m_converter = new Mol2Conv();
		} else if (cs.getType().equals(CMLCell.TYPE)) {
			m_converter = new CMLConv();
		} else {
			m_converter = new SmilesConv();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell getCell(final DataRow row) {

		try {
			DataCell cell = row.getCell(m_colIndex);
			if (cell.isMissing()) {
				return cell;
			} else {
				return m_converter.conv(((CDKValue) row.getCell(m_colIndex)).getAtomContainer());
			}
		} catch (Exception ex) {
			LOGGER.error("Could not convert molecules: " + ex.getMessage(), ex);
			return DataType.getMissingCell();
		}
	}
}
