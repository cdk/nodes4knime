/* Created on 25.03.2007 16:49:50 by thor
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
package org.openscience.cdk.knime;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initializer for KNIME's CDK plugin preferences.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDKPreferenceInitializer extends AbstractPreferenceInitializer {
    /** Preference key for the "show implicit hydrogens" setting. */
//    public static final String PREF_SHOW_IMPLICIT_HYDROGENS =
//            "knime.cdk.implicitHydrogens";

    /** Preference key for the "show explicit hydrogens" setting. */
    public static final String PREF_SHOW_EXPLICIT_HYDROGENS =
            "knime.cdk.explicitHydrogens";
    
    /** Preference key for the "use multiple threads" setting. */
    public static final String PREF_USE_MULTIPLE_THREADS =
    		"knime.cdk.multipleThreads";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        IPreferenceStore store =
                CDKNodePlugin.getDefault().getPreferenceStore();

        // set default values
//        store.setDefault(PREF_SHOW_IMPLICIT_HYDROGENS, false);
        store.setDefault(PREF_SHOW_EXPLICIT_HYDROGENS, false);
        store.setDefault(PREF_USE_MULTIPLE_THREADS, false);
    }
}
