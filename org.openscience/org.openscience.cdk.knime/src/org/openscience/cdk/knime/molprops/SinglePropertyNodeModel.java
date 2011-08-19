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
 * History
 *   Feb 2, 2007 (wiswedel): created
 */
package org.openscience.cdk.knime.molprops;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SinglePropertyNodeModel extends NodeModel {
    
    /** Config key for cdk column. */
    static final String CFG_CDK_COL = "cdkColumn";
    
    private final SettingsModelString m_cdkColSelModel;
    private final String m_descriptorClassName;
    
    /** Inits super with one input, one output.
     * @param descriptorClassName The class name of the CDK descriptor. */
    public SinglePropertyNodeModel(final String descriptorClassName) {
        super(1, 1);
        m_cdkColSelModel = createColSelectorSettingsModel();
        m_descriptorClassName = descriptorClassName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String name = m_cdkColSelModel.getStringValue();
        if (name == null) {
            for (DataColumnSpec c : inSpecs[0]) {
                if (c.getType().isCompatible(CDKValue.class)) {
                    name = c.getName();
                }
            }
            if (name != null) {
                m_cdkColSelModel.setStringValue(name);
                setWarningMessage("Auto configuration: using column \"" 
                        + name + "\".");
            } else {
                throw new InvalidSettingsException("No CDK compatible column "
                        + "in input table");
            }
        }
        ColumnRearranger rearranger = createColumnRearranger(inSpecs[0]);
        return new DataTableSpec[]{rearranger.createSpec()};
    }
    
    private ColumnRearranger createColumnRearranger(final DataTableSpec in)
        throws InvalidSettingsException {
        String name = m_cdkColSelModel.getStringValue();
        int cdkColIndex = in.findColumnIndex(name);
        if (cdkColIndex < 0) {
            throw new InvalidSettingsException("No such column \"" + name 
                    + "\" in input table.");
        }
        DataColumnSpec cdkColSpec = in.getColumnSpec(cdkColIndex); 
        if (!cdkColSpec.getType().isCompatible(CDKValue.class)) {
            throw new InvalidSettingsException("Column \"" + name 
                    + "\" does not contain CDK molecules"); 
        }
        DataColumnSpec appendSpec = 
            MolPropsLibrary.getColumnSpec(m_descriptorClassName);
        String colName = appendSpec.getName();
        int uniquifier = 1;
        while (in.containsName(colName)) {
            colName = appendSpec.getName() + " #" + uniquifier++; 
        }
        if (uniquifier > 1) {
            DataColumnSpecCreator c = new DataColumnSpecCreator(appendSpec);
            c.setName(colName);
            appendSpec = c.createSpec();
        }
        MolPropsGenerator generator = new MolPropsGenerator(cdkColIndex, 
                new String[]{m_descriptorClassName}, 
                new DataColumnSpec[]{appendSpec});
        ColumnRearranger rearrange = new ColumnRearranger(in);
        rearrange.append(generator);
        return rearrange;
    }
    
    /** @see NodeModel#execute(BufferedDataTable[], ExecutionContext) */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, 
            final ExecutionContext exec) throws Exception {
        ColumnRearranger rearranger = 
            createColumnRearranger(inData[0].getDataTableSpec());
        BufferedDataTable out = exec.createColumnRearrangeTable(
                inData[0], rearranger, exec);
        return new BufferedDataTable[]{out};
    }

    /** @see NodeModel#reset() */
    @Override
    protected void reset() {
    }
    
    /** @see org.knime.core.node.NodeModel#loadInternals(File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** @see NodeModel#saveInternals(File, ExecutionMonitor) */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO) */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
    throws InvalidSettingsException {
        m_cdkColSelModel.loadSettingsFrom(settings);
    }
    
    /** @see NodeModel#saveSettingsTo(NodeSettingsWO) */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_cdkColSelModel.saveSettingsTo(settings);
    }

    /** @see NodeModel#validateSettings(NodeSettingsRO) */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_cdkColSelModel.validateSettings(settings);
    }
    
    /** Factory method for the settings holder to be used in NodeModel and
     * NodeDialogPane. 
     * @return A new settings model.
     */
    static SettingsModelString createColSelectorSettingsModel() {
        return new SettingsModelString(CFG_CDK_COL, null);
    }

}
