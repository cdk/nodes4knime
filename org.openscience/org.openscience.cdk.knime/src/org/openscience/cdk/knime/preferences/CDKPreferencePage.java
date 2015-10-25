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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.openscience.cdk.knime.CDKNodePlugin;

/**
 * This is the preference page for KNIME's CDK plugin.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public enum NUMBERING {
		NONE, CANONICAL, SEQUENTIAL
	};

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

		Composite parent = getFieldEditorParent();

		String[][] numberingLabelsAndValues = new String[][] { 
				{ "None", NUMBERING.NONE.name() },
				{ "Canonical", NUMBERING.CANONICAL.name() },
				{ "Sequential", NUMBERING.SEQUENTIAL.name() } };

		RadioGroupFieldEditor numbering = new RadioGroupFieldEditor(CDKPreferenceInitializer.NUMBERING_TYPE,
				"Atom number type: ", 1, numberingLabelsAndValues, parent);

		addField(numbering);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// nothing to do
	}
}
