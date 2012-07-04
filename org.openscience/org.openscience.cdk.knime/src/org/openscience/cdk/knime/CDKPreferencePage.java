/*
 * Created on 25.03.2007 16:52:27 by thor
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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This is the preference page for KNIME's CDK plugin.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Creates a new preference page.
	 */
	public CDKPreferencePage() {

		super(GRID);

		// we use the pref store of the UI plugin
		setPreferenceStore(CDKNodePlugin.getDefault().getPreferenceStore());
		setDescription("KNIME CDK preferences");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFieldEditors() {

		// Composite parent = getFieldEditorParent();

		// BooleanFieldEditor showImplicitHydrogens =
		// new BooleanFieldEditor(
		// CDKPreferenceInitializer.PREF_SHOW_IMPLICIT_HYDROGENS,
		// "Show implicit hydrogens in structure diagrams", parent);
		// addField(showImplicitHydrogens);

		// BooleanFieldEditor showExplicitHydrogens =
		// new BooleanFieldEditor(
		// CDKPreferenceInitializer.PREF_SHOW_EXPLICIT_HYDROGENS,
		// "Show explicit hydrogens in structure diagrams", parent);
		// addField(showExplicitHydrogens);

		// BooleanFieldEditor useMultipleThreads =
		// new BooleanFieldEditor(
		// CDKPreferenceInitializer.PREF_USE_MULTIPLE_THREADS,
		// "Use CDK with multiple threads", parent);
		// addField(useMultipleThreads);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {

		// nothing to do
	}
}
