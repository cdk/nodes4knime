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
 *   Mar 23, 2006 (wiswedel): created
 */
package org.openscience.cdk.knime.view3d;

import java.io.File;
import java.io.IOException;

import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.tableview.TableContentModel;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerNodeModel extends NodeModel 
    implements BufferedDataTableHolder {
    
    private int m_structureColumn = -1;
    private final TableContentModel m_contentModel;
    private int m_structureType = JmolViewerNodeView.UNKNOWN_TYPE;
    
    /** Public constructor */
    public JmolViewerNodeModel() {
        super(1, 0);
        m_contentModel = new TableContentModel();
    }
    
    /** Get the index of the selected structure column.
     * @return The structure column (SDF or CDK) or -1 if none has been 
     * selected.
     */
    int getStructureColumn() {
        return m_structureColumn;
    }
    
    /** @return type of structure, e.g. JMolViewerNodeView#SDF_TYPE 
     */
    int getStructureType() {
        return m_structureType;
    }
    
    /** Get reference to the table model.
     * @return The table model to be displayed on top.     
     */
    TableContentModel getContentModel() {
        return m_contentModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData, ExecutionContext exec)
            throws Exception {
        setInternalTables(inData);
        return new BufferedDataTable[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_contentModel.setDataTable(null);
        m_contentModel.setHiLiteHandler(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        DataTableSpec inSpec = inSpecs[0];
        if (!updateStructureColumnAndType(inSpec)) {
            throw new InvalidSettingsException("Input data does neither " 
                    + "contain CDK-column nor Sdf column");
        }
        return new DataTableSpec[0];
    }
    
    /** Scans the argument spec for appropriate columns. Resets internal.
     * fields if the argument is null.
     * @param inSpec The new input spec.
     * @return <code>false</code> if the argument is not null and does not 
     * contain an appropriate column. 
     */
    private boolean updateStructureColumnAndType(final DataTableSpec inSpec) {
        if (inSpec == null) {
            m_structureColumn = -1;
            return false;
        }
        int firstStructureColumn = -1;
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            if (colSpec.getType().isCompatible(SdfValue.class)) {
                firstStructureColumn = i;
                m_structureType = JmolViewerNodeView.SDF_TYPE;
                break;
            } else if (colSpec.getType().isCompatible(CDKValue.class)) {
                firstStructureColumn = i;
                m_structureType = JmolViewerNodeView.CDK_TYPE;
                break;
            }
        }
        m_structureColumn = firstStructureColumn;
        if (firstStructureColumn < 0) {
            return false;
        }
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public BufferedDataTable[] getInternalTables() {
        return new BufferedDataTable[]{
                (BufferedDataTable)m_contentModel.getDataTable()};
    }
    
    /** {@inheritDoc} */
    @Override
    public void setInternalTables(final BufferedDataTable[] tables) {
        if (tables[0] != null) {
            updateStructureColumnAndType(tables[0].getDataTableSpec());
        }
        m_contentModel.setDataTable(tables[0]);
        m_contentModel.setHiLiteHandler(getInHiLiteHandler(0));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
        
    }

}
