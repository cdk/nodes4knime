/* Created on 31.01.2007 18:34:35 by thor
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
package org.openscience.cdk.knime.sssearch;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.knime.util.JMolSketcherPanel;

/**
 * This is the dialog for the substructre search node. It lets the user draw a
 * fragment and choose the column containing the molecules.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class SSSearchNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(SSSearchNodeDialog.class);

    private final JMolSketcherPanel m_panel = new JMolSketcherPanel();

    private final ColumnSelectionComboxBox m_molColumnName =
            new ColumnSelectionComboxBox((Border)null, CDKValue.class);

    private SSSearchSettings m_settings = new SSSearchSettings();

    /**
     * Creates a new dialog.
     */
    public SSSearchNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        p.add(m_panel, c);

        c.gridy++;
        c.ipady = 5;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = 1;
        p.add(new JLabel("Column with molecules   "), c);
        c.gridx = 1;
        p.add(m_molColumnName, c);

        addTab("JChemPaint", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        try {
            m_settings.loadSettings(settings);
        } catch (InvalidSettingsException ex) {
            // ignore it and use defaults
        }

        if (m_settings.smilesFragments() != null) {
            try {
                m_panel.loadStructures(m_settings.smilesFragments());
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                throw new NotConfigurableException(ex.getMessage());
            }
        }

        m_molColumnName.update(specs[0], m_settings.molColName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.smilesFragments(m_panel.getAllSmiles());
        m_settings.molColName(m_molColumnName.getSelectedColumn());
        m_settings.saveSettings(settings);
    }
}
