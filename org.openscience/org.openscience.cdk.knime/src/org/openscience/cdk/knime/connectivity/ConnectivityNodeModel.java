/* Created on 30.01.2007 16:54:46 by thor
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
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.connectivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model for the connectivity node that performs all computation by
 * using CDK functionality.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeModel extends NodeModel {
    private final ConnectivitySettings m_settings = new ConnectivitySettings();

    /**
     * Creates a new model with one input and one output port.
     */
    public ConnectivityNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
        if (molCol == -1) {
            for (DataColumnSpec dcs : inSpecs[0]) {
                if (dcs.getType().isCompatible(CDKValue.class)) {
                    if (molCol >= 0) {
                        molCol = -1;
                        break;
                    } else {
                        molCol = inSpecs[0].findColumnIndex(dcs.getName());
                    }
                }
            }

            if (molCol != -1) {
                String name = inSpecs[0].getColumnSpec(molCol).getName();
                setWarningMessage("Using '" + name + "' as molecule column");
                m_settings.molColumnName(name);
            }
        }

        if (molCol == -1) {
            throw new InvalidSettingsException("Molecule column '"
                    + m_settings.molColumnName() + "' does not exist");
        }

        if (m_settings.addFragmentColumn()) {
            String name =
                    DataTableSpec.getUniqueColumnName(inSpecs[0], "Fragments");
            DataColumnSpec cs =
                    new DataColumnSpecCreator(name,
                            SetCell.getCollectionType(CDKCell.TYPE))
                            .createSpec();
            return new DataTableSpec[]{AppendedColumnTable.getTableSpec(
                    inSpecs[0], cs)};
        } else {
            return inSpecs;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int molColIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.molColumnName());

        BufferedDataTable res;
        if (m_settings.removeCompleteRow()) {
            res = removeRows(inData[0], exec, molColIndex);
        } else if (m_settings.addFragmentColumn()) {
            res = addFragments(inData[0], exec, molColIndex);
        } else {
            res = retainBiggest(inData[0], exec, molColIndex);
        }

        return new BufferedDataTable[]{res};
    }

    private BufferedDataTable removeRows(final BufferedDataTable in,
            final ExecutionContext exec, final int molColIndex) {
        final double max = in.getRowCount();

        BufferedDataContainer cont =
                exec.createDataContainer(in.getDataTableSpec());
        int count = 0;
        int removed = 0;
        exec.setMessage("");
        for (DataRow row : in) {
            exec.setProgress(count++ / max);
            if (row.getCell(molColIndex).isMissing()) {
                cont.addRowToTable(row);
                continue;
            }

            IAtomContainer mol = null;
            try {
                mol = (IAtomContainer)
                        ((CDKValue)row.getCell(molColIndex))
                                .getAtomContainer().clone();
            } catch (CloneNotSupportedException exception) {
            	setWarningMessage("Clone not supported.");
            }

            if (ConnectivityChecker.isConnected(mol)) {
                cont.addRowToTable(row);
            } else {
                exec.setMessage("Removed " + ++removed + " molecules");
            }
        }
        cont.close();

        return cont.getTable();
    }

    private BufferedDataTable retainBiggest(final BufferedDataTable in,
            final ExecutionContext exec, final int molColIndex)
            throws CanceledExecutionException {
        ColumnRearranger crea = new ColumnRearranger(in.getDataTableSpec());
        SingleCellFactory cf =
                new SingleCellFactory(in.getDataTableSpec().getColumnSpec(
                        molColIndex)) {
                    @Override
                    public DataCell getCell(final DataRow row) {
                        if (row.getCell(molColIndex).isMissing()) {
                            return DataType.getMissingCell();
                        }

                        IAtomContainer mol = null;
                        try {
	                        mol = (IAtomContainer)
	                                ((CDKValue)row.getCell(molColIndex))
	                                        .getAtomContainer().clone();
                        } catch (CloneNotSupportedException exception) {
                        	setWarningMessage("Clone not supported.");
                        }

                        if (!ConnectivityChecker.isConnected(mol)) {
                            IMoleculeSet molSet =
                                    ConnectivityChecker
                                            .partitionIntoMolecules(mol);
                            IMolecule biggest = molSet.getMolecule(0);
                            for (int i = 1; i < molSet.getMoleculeCount(); i++) {
                                if (molSet.getMolecule(i).getBondCount() > biggest
                                        .getBondCount()) {
                                    biggest = molSet.getMolecule(i);
                                }
                            }

                            return new CDKCell(biggest);
                        } else {
                            return row.getCell(molColIndex);
                        }
                    }
                };
        crea.replace(cf, molColIndex);

        return exec.createColumnRearrangeTable(in, crea, exec);
    }

    private BufferedDataTable addFragments(final BufferedDataTable in,
            final ExecutionContext exec, final int molColIndex)
            throws CanceledExecutionException {
        ColumnRearranger crea = new ColumnRearranger(in.getDataTableSpec());

        String name =
                DataTableSpec.getUniqueColumnName(in.getDataTableSpec(),
                        "Fragments");
        DataColumnSpec cs =
                new DataColumnSpecCreator(name,
                        SetCell.getCollectionType(CDKCell.TYPE)).createSpec();

        SingleCellFactory cf = new SingleCellFactory(cs) {
            @Override
            public DataCell getCell(final DataRow row) {
                if (row.getCell(molColIndex).isMissing()) {
                    return DataType.getMissingCell();
                }

                IAtomContainer mol =
                        ((CDKValue)row.getCell(molColIndex)).getAtomContainer();

                if (!ConnectivityChecker.isConnected(mol)) {
                    IMoleculeSet molSet =
                            ConnectivityChecker.partitionIntoMolecules(mol);
                    List<DataCell> cells =
                            new ArrayList<DataCell>(molSet.getMoleculeCount());

                    for (int i = 0; i < molSet.getMoleculeCount(); i++) {
                        cells.add(new CDKCell(molSet.getMolecule(i)));
                    }

                    return CollectionCellFactory.createSetCell(cells);
                } else {
                    return CollectionCellFactory.createSetCell(Collections
                            .singleton(row.getCell(molColIndex)));
                }
            }
        };
        crea.append(cf);

        return exec.createColumnRearrangeTable(in, crea, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ConnectivitySettings s = new ConnectivitySettings();
        s.loadSettings(settings);
        if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
            throw new InvalidSettingsException("No molecule column chosen");
        }
    }
}
