/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   16.09.2008 (thor): created
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
import org.openscience.cdk.Molecule;
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
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(CDK2MoleculeNodeModel.class);

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
//        	AtomContainerManipulator.clearAtomConfigurations(molClone);

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
            writer.writeMolecule(new Molecule(mol));
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
            writer.writeMolecule(new Molecule(mol));
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
                return m_converter.conv(((CDKValue)row.getCell(m_colIndex))
                        .getAtomContainer());
            }
        } catch (Exception ex) {
            LOGGER.error("Could not convert molecules: " + ex.getMessage(), ex);
            return DataType.getMissingCell();
        }
    }

}
