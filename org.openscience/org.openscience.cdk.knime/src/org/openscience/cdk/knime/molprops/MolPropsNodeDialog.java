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

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilterPanel;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.openscience.cdk.knime.type.CDKValue;


/**
 * Dialog to choose the molecular properties to be calculated.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MolPropsNodeDialog extends NodeDialogPane {

    private final ColumnSelectionPanel m_selPanel;

    private final ColumnFilterPanel m_filterPanel;

    /**
     * Inits GUI.
     */
    @SuppressWarnings("unchecked")
    MolPropsNodeDialog() {
        m_selPanel = new ColumnSelectionPanel(CDKValue.class);
        m_filterPanel = new ColumnFilterPanel(false);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(m_selPanel, BorderLayout.NORTH);
        panel.add(m_filterPanel, BorderLayout.CENTER);
        addTab("Properties and target column", panel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        String a = settings.getString(MolPropsNodeModel.CFGKEY_SMILES, null);
        String[] selProps = settings.getStringArray(
                MolPropsNodeModel.CFGKEY_PROPS, (String[])null);
        m_selPanel.update(specs[0], a);
        DataTableSpec dummySpec = new DataTableSpec(
                MolPropsNodeModel.getAvailableDescriptorList());
        m_filterPanel.update(dummySpec, false, selProps);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        String smilesCell = m_selPanel.getSelectedColumn();
        settings.addString(MolPropsNodeModel.CFGKEY_SMILES, smilesCell);
        String[] selProps = m_filterPanel.getIncludedColumnSet().toArray(
                new String[0]);
        settings.addStringArray(MolPropsNodeModel.CFGKEY_PROPS, selProps);
    }
}
