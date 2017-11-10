/*
 * Copyright (C) 2003 - 2016 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This file is part of the KNIME CDK plugin.
 * 
 * The KNIME CDK plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The KNIME CDK plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with the plugin. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.knime.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.NUMBERING;

/**
 * Initializer for KNIME's CDK plugin preferences.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKPreferenceInitializer extends AbstractPreferenceInitializer {

	/** Preference key for the "numbering type" setting. */
	public static final String NUMBERING_TYPE = "knime.cdk.numbering";
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeDefaultPreferences() {

		// get the preference store for the UI plugin
		IPreferenceStore store = CDKNodePlugin.getDefault().getPreferenceStore();

		// set default values
		store.setDefault(NUMBERING_TYPE, NUMBERING.NONE.name());
	}
}
