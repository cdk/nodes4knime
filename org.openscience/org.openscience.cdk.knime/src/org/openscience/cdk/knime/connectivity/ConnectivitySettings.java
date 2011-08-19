/* Created on 30.01.2007 17:33:09 by thor
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class stores the settings for the connectivity node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivitySettings {
    private String m_molColumnName;

    private boolean m_removeCompleteRow;

    private boolean m_addFragmentColumn;

    /**
     * Returns the name of the column containing the molecules.
     *
     * @return the molecules' column name
     */
    public String molColumnName() {
        return m_molColumnName;
    }

    /**
     * Sets the name of the column containing the molecules.
     *
     * @param colName the molecules' column name
     */
    public void molColumnName(final String colName) {
        m_molColumnName = colName;
    }

    /**
     * Returns if rows containing fragmented molecules should be removed
     * completely or only all fragments except the biggest one.
     *
     * @return <code>true</code> if the whole row should be removed,
     *         <code>false</code> if only the small fragments should be
     *         removed.
     */
    public boolean removeCompleteRow() {
        return m_removeCompleteRow;
    }

    /**
     * Sets if rows containing fragmented molecules should be removed completely
     * or only all fragments except the biggest one.
     *
     * @param removeCompleteRow <code>true</code> if the whole row should be
     *            removed, <code>false</code> if only the small fragments
     *            should be removed.
     */
    public void removeCompleteRow(final boolean removeCompleteRow) {
        m_removeCompleteRow = removeCompleteRow;
    }

    /**
     * Returns if a column with all fragments should be added.
     *
     * @return <code>true</code> if a column should be added,
     *         <code>false</code> otherwise
     */
    public boolean addFragmentColumn() {
        return m_addFragmentColumn;
    }

    /**
     * Sets if a column with all fragments should be added.
     *
     * @param add <code>true</code> if a column should be added,
     *         <code>false</code> otherwise
     */
    public void addFragmentColumn(final boolean add) {
        m_addFragmentColumn = add;
    }

    /**
     * Saves the settings into the given node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("molColumn", m_molColumnName);
        settings.addBoolean("removeCompleteRow", m_removeCompleteRow);
        settings.addBoolean("addFragmentColumn", m_addFragmentColumn);
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if not all required settings are
     *             available
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_molColumnName = settings.getString("molColumn");
        m_removeCompleteRow = settings.getBoolean("removeCompleteRow");
        m_addFragmentColumn = settings.getBoolean("addFragmentColumn", false);
    }
}
