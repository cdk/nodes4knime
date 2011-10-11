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
 * -------------------------------------------------------------------
 *
 */
package org.openscience.cdk.knime.molprops;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKValue;


/**
 * Factory that generates molecular properties.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
final class MolPropsGenerator implements CellFactory {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(MolPropsGenerator.class);

    private final DataColumnSpec[] m_properties;
    private final String[] m_descClassNames;

    private final int m_smilesColIndex;

    /**
     * Init this generator.
     *
     * @param smilesIndex the index where to find the smiles cell
     * @param classNames The internal identifiers for the descriptors
     * @param props properties to generate
     * @see MolPropsLibrary#getPropsDescription()
     */
    MolPropsGenerator(final int smilesIndex,
            final String[] classNames, final DataColumnSpec[] props) {
        if (classNames.length != props.length) {
            throw new IndexOutOfBoundsException("Non matching lengths: "
                    + classNames.length + " vs. " + props.length);
        }
        m_smilesColIndex = smilesIndex;
        m_descClassNames = classNames;
        m_properties = props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        DataCell sCell = row.getCell(m_smilesColIndex);
        DataCell[] newCells = new DataCell[m_properties.length];
        if (sCell.isMissing()) {
            Arrays.fill(newCells, DataType.getMissingCell());
            return newCells;
        }
        if (!(sCell instanceof CDKValue)) {
            throw new IllegalArgumentException("No CDK cell at "
                    + m_smilesColIndex + ": " + sCell.getClass().getName());
        }
        IAtomContainer mol = null;
        try {
        	mol = (IAtomContainer) ((CDKValue)sCell).getAtomContainer().clone();
            CDKHueckelAromaticityDetector.detectAromaticity(mol);
        } catch (CDKException ce) {
            LOGGER.debug("Unable to carry out ring detection on molecule in "
                    + "row " + "\"" + row.getKey() + "\"", ce);
        } catch (CloneNotSupportedException ce) {
        	LOGGER.debug("Unable to clone molecule in row \"" + row.getKey() + "\"", ce);
        }


        for (int i = 0; i < m_descClassNames.length; i++) {
            String prop = m_descClassNames[i];
            newCells[i] = MolPropsLibrary.getProperty(row.getKey().toString(),
                    mol, prop);
        }
        return newCells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec[] getColumnSpecs() {
        return m_properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProgress(final int curRowNr, final int rowCount,
            final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress(curRowNr / (double)rowCount,
                "Calculated properties for row " + curRowNr
                + " (\"" + lastKey + "\")");
    }
}
