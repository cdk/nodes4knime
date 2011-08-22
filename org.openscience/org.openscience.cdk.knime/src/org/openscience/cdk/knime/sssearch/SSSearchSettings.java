/* Created on 01.02.2007 10:17:50 by thor
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds all necessary settings for the substructure search node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class SSSearchSettings {
    private String[] m_smilesFragments;

    private String m_molColumnName;

    /**
     * Returns Smiles strings containing the fragment.
     * 
     * @return the fragments in Smiles
     */
    public String[] smilesFragments() {
        return m_smilesFragments;
    }

    /**
     * Sets the Smiles strings containing the fragments.
     * 
     * @param cml the fragments in Smiles
     */
    public void smilesFragments(final String... cml) {
        m_smilesFragments = cml;
    }

    /**
     * Returns the name of the column containing the molecules.
     * 
     * @return the molecules' column name
     */
    public String molColName() {
        return m_molColumnName;
    }

    /**
     * Sets the name of the column containing the molecules.
     * 
     * @param colName the molecules' column name
     */
    public void molColName(final String colName) {
        m_molColumnName = colName;
    }

    /**
     * Saves the settings into the given node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("molColumn", m_molColumnName);
        settings.addStringArray("fragments", m_smilesFragments);
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
        m_smilesFragments = settings.getStringArray("fragments");
    }
}
