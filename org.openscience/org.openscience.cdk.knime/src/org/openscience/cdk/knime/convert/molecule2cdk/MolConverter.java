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
 *   12.09.2008 (thor): created
 */
package org.openscience.cdk.knime.convert.molecule2cdk;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pointer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.Mol2Reader;
import org.openscience.cdk.knime.convert.TimeoutThreadPool;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Helper class for converting string representations into CDK molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
class MolConverter implements ExtendedCellFactory {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(Molecule2CDKNodeModel.class);

    private interface Conv {
        /**
         * Converts a molecule's string representation into a CDK object.
         *
         * @param cell a data cell with a molecule string
         * @return a CDK molecule
         * @throws CDKException if an error occurs during conversion
         */
        public IMolecule conv(DataCell cell) throws CDKException;
    }

    private class SdfConv implements Conv {
        /**
         * {@inheritDoc}
         */
        @Override
        public IMolecule conv(final DataCell cell) throws CDKException {
            String sdf = ((SdfValue)cell).getSdfValue();

            MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdf));
            return reader.read(new Molecule());
        }
    }

    private class MolConv implements Conv {
        /**
         * {@inheritDoc}
         */
        @Override
        public IMolecule conv(final DataCell cell) throws CDKException {
            String mol = ((MolValue)cell).getMolValue();

            MDLReader reader = new MDLReader(new StringReader(mol));
            return (IMolecule) reader.read(new Molecule());
        }
    }

    private class Mol2Conv implements Conv {
        /**
         * {@inheritDoc}
         */
        @Override
        public IMolecule conv(final DataCell cell) throws CDKException {
            String mol2 = ((Mol2Value)cell).getMol2Value();

            Mol2Reader reader = new Mol2Reader(new StringReader(mol2));
            return reader.read(new Molecule());
        }
    }

    private class CMLConv implements Conv {
        /**
         * {@inheritDoc}
         */
        @Override
        public IMolecule conv(final DataCell cell) throws CDKException {
            String cml = ((CMLValue)cell).getCMLValue();

            CMLReader reader =
                    new CMLReader(new ByteArrayInputStream(cml.getBytes()));
            return (IMolecule) reader.read(new Molecule());
        }
    }

    private class SmilesConv implements Conv {
        /**
         * {@inheritDoc}
         */
        @Override
        public IMolecule conv(final DataCell cell) throws CDKException {
            final String smiles = ((SmilesValue)cell).getSmilesValue();

            final SmilesParser parser =
                    new SmilesParser(
                            NoNotificationChemObjectBuilder.getInstance());
            return parser.parseSmiles(smiles);
        }
    }

    private final ColumnDestination[] m_colDest;

    private final DataColumnSpec[] m_colSpec;

    private final Molecule2CDKSettings m_settings;

    private final int m_colIndex;

    private final Conv m_converter;

    private final TimeoutThreadPool m_pool;

    /**
     * Creates a new converter.
     *
     * @param inSpec the spec of the input table
     * @param settings the settings of the converter node
     * @param pool the thread pool that should be used for converting
     */
    public MolConverter(final DataTableSpec inSpec,
            final Molecule2CDKSettings settings, final TimeoutThreadPool pool) {
        m_colIndex = inSpec.findColumnIndex(settings.columnName());
        if (settings.replaceColumn()) {
            m_colSpec =
                    new DataColumnSpec[]{new DataColumnSpecCreator(
                            settings.columnName(), CDKCell.TYPE).createSpec()};
            m_colDest = new ColumnDestination[]{new ReplaceColumn(m_colIndex)};
        } else {
            m_colSpec =
                    new DataColumnSpec[]{new DataColumnSpecCreator(
                            DataTableSpec.getUniqueColumnName(inSpec,
                                    settings.newColumnName()), CDKCell.TYPE)
                            .createSpec()};
            m_colDest = new ColumnDestination[]{new AppendColumn()};
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
        } else {
            m_converter = new SmilesConv();
        }

        m_settings = settings;
        m_pool = pool;
    }

    @Override
    public DataCell[] getCells(final DataRow row) {
        final DataCell cell = row.getCell(m_colIndex);

        if (cell.isMissing()) {
            return new DataCell[]{DataType.getMissingCell()};
        }

        final Pointer<IMolecule> molP = new Pointer<IMolecule>();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                runWithTimeout(cell, molP);
            }
        };
        try {
            if (!m_pool.run(r, m_settings.timeout())) {
                LOGGER.error("Timeout while converting molecule "
                        + row.getKey());
                return new DataCell[]{DataType.getMissingCell()};
            } else if (molP.get() == null) {
                return new DataCell[]{DataType.getMissingCell()};
            }
        } catch (InterruptedException ex) {
            LOGGER.error("Error converting molecule: " + ex.getMessage(), ex);
            return new DataCell[]{DataType.getMissingCell()};
        }

        if (molP.get().getID() == null) {
            if (molP.get().getProperty(CDKConstants.TITLE) != null) {
                molP.get().setID(
                        molP.get().getProperty(CDKConstants.TITLE).toString());
            } else {
                molP.get().setID(row.getKey().toString());
            }
        }

        return new DataCell[]{new CDKCell(molP.get())};
    }

    private void runWithTimeout(final DataCell cell,
            final Pointer<IMolecule> mol) {
        try {
            IMolecule cdkMol = m_converter.conv(cell);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(cdkMol);

            if (m_settings.addHydrogens()) {
                CDKHydrogenAdder hyda =
                        CDKHydrogenAdder
                                .getInstance(NoNotificationChemObjectBuilder
                                        .getInstance());
                hyda.addImplicitHydrogens(cdkMol);
                AtomContainerManipulator
                        .convertImplicitToExplicitHydrogens(cdkMol);
            }

            if (m_settings.generate2D()) {
                if (m_settings.force2D()
                        || (GeometryTools.has2DCoordinatesNew(cdkMol) != 2)) {
                    if (!ConnectivityChecker.isConnected(cdkMol)) {
                        IMoleculeSet set =
                                ConnectivityChecker
                                        .partitionIntoMolecules(cdkMol);
                        // the selection of the biggest fragment should not be
                        // carried out by this worker by default
                        // e.g., counter ions may be of importance
                        // IMolecule biggest = set.getMolecule(0);
                        // for (int i = 1; i < set.getMoleculeCount(); i++) {
                        // if (set.getMolecule(i).getBondCount() > biggest
                        // .getBondCount()) {
                        // biggest = set.getMolecule(i);
                        // }
                        // }
                        // new StructureDiagramGenerator(biggest)
                        // .generateCoordinates();
                        for (int i = 0; i < set.getMoleculeCount(); i++) {
                            new StructureDiagramGenerator(set.getMolecule(i))
                                    .generateCoordinates();
                        }
                    } else {
                        new StructureDiagramGenerator(cdkMol)
                                .generateCoordinates();
                    }
                }
            }
            mol.set(cdkMol);
        } catch (Exception ex) {
            mol.set(null);
            LOGGER.error("Could not convert molecule: " + ex.getMessage(), ex);
        }
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
