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

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.replace.ReplacedColumnsTable;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.convert.TimeoutThreadPool;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This is the model for the Molecule->CDK node that converts molecules' string
 * representations into CDK objects. The conversion is done in parallel because
 * the model optionally also computes 2D coordinates and parsing Smiles may take
 * a long time by itself.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class Molecule2CDKNodeModel extends ThreadedColAppenderNodeModel {
    private final Molecule2CDKSettings m_settings = new Molecule2CDKSettings();

    /**
     * Creates a new model.
     */
    public Molecule2CDKNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExtendedCellFactory[] prepareExecute(final DataTable[] data)
            throws Exception {
        TimeoutThreadPool pool = new TimeoutThreadPool();
        return new ExtendedCellFactory[]{new MolConverter(data[0]
                .getDataTableSpec(), m_settings, pool)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        int colIndex = inSpecs[0].findColumnIndex(m_settings.columnName());
        if (colIndex == -1) {
            int index = 0;
            for (DataColumnSpec spec : inSpecs[0]) {
                DataType t = spec.getType();
                if (t.isCompatible(SdfValue.class)
                        || t.isCompatible(MolValue.class)
                        || t.isCompatible(SmilesValue.class)
                        || t.isCompatible(Mol2Value.class)) {
                    if (colIndex != -1) {
                        setWarningMessage("Auto-selected column '"
                                + spec.getName() + "'");
                    }
                    colIndex = index;
                }
                index++;
            }
            if (colIndex == -1) {
                throw new InvalidSettingsException("No molecule column found");
            }
            m_settings.columnName(inSpecs[0].getColumnSpec(colIndex).getName());
        } else {
            DataType t = inSpecs[0].getColumnSpec(colIndex).getType();
            if (!(t.isCompatible(SdfValue.class)
                    || t.isCompatible(MolValue.class)
                    || t.isCompatible(SmilesValue.class) || t
                    .isCompatible(Mol2Value.class))) {
                throw new InvalidSettingsException("Column '"
                        + m_settings.columnName()
                        + "' is not a supported molecule column");
            }
        }

        DataTableSpec outSpec;
        if (m_settings.replaceColumn()) {
            DataColumnSpecCreator crea =
                    new DataColumnSpecCreator(m_settings.columnName(),
                            CDKCell.TYPE);
            outSpec =
                    ReplacedColumnsTable.createTableSpec(inSpecs[0], crea
                            .createSpec(), colIndex);
        } else {
            DataColumnSpecCreator crea =
                    new DataColumnSpecCreator(DataTableSpec
                            .getUniqueColumnName(inSpecs[0], m_settings
                                    .newColumnName()), CDKCell.TYPE);
            outSpec =
                    AppendedColumnTable.getTableSpec(inSpecs[0], crea
                            .createSpec());
        }

        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
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
        Molecule2CDKSettings s = new Molecule2CDKSettings();
        s.loadSettings(settings);
        if (!s.replaceColumn()
                && ((s.newColumnName() == null) || (s.newColumnName().length() < 1))) {
            throw new InvalidSettingsException("No name for new column given");
        }
    }
}
