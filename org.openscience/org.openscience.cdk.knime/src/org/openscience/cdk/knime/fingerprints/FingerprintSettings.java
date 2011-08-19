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
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.fingerprints;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the fingerprint node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class FingerprintSettings {
    /** Enum for the different fingerprint types. */
    public enum FingerprintTypes {
        Standard, Extended, EState, MACCS, Pubchem
    }

    private String m_molColumn = null;

    private FingerprintTypes m_fingerprintType = FingerprintTypes.Standard;

    /**
     * Returns the name of the column that holds the molecules.
     *
     * @return a column name
     */
    public String molColumn() {
        return m_molColumn;
    }

    /**
     * Sets the name of the column that holds the molecules.
     *
     * @param columnName a column name
     */
    public void molColumn(final String columnName) {
        m_molColumn = columnName;
    }

    /**
     * Returns the type of fingerprints that should be generated.
     *
     * @return the type of fingerprint
     */
    public FingerprintTypes fingerprintType() {
        return m_fingerprintType;
    }

    /**
     * Sets the type of fingerprints that should be generated.
     *
     * @param type the type of fingerprint
     */
    public void fingerprintType(final FingerprintTypes type) {
        m_fingerprintType = type;
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_molColumn = settings.getString("molColumn", null);
        m_fingerprintType =
                FingerprintTypes.valueOf(settings.getString("fingerprintType",
                        FingerprintTypes.Standard.toString()));
    }

    /**
     * Loads the settings from the given node settings object.
     *
     * @param settings node settings
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_molColumn = settings.getString("molColumn");
        m_fingerprintType =
                FingerprintTypes.valueOf(settings.getString("fingerprintType"));
    }

    /**
     * Saves the settings to the given node settings object.
     *
     * @param settings node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addString("molColumn", m_molColumn);
        settings.addString("fingerprintType", m_fingerprintType.toString());
    }
}
