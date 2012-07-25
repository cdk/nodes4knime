/*
 * Created on 25.03.2007 16:49:50 by thor
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.openscience.cdk.knime.CDKPreferencePage.LABELS;

/**
 * Initializer for KNIME's CDK plugin preferences.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKPreferenceInitializer extends AbstractPreferenceInitializer {

	/** Preference key for the "show numbers" setting. */
	 public static final String SHOW_NUMBERS = "knime.cdk.numbers";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initializeDefaultPreferences() {

		// get the preference store for the UI plugin
		 IPreferenceStore store = CDKNodePlugin.getDefault().getPreferenceStore();

		// set default values
		 store.setDefault(SHOW_NUMBERS, LABELS.NONE.name());
	}
}
