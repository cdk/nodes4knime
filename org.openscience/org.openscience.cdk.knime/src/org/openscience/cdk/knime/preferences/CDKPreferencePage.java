/*
 * Created on 25.03.2007 16:52:27 by thor
 * 
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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

	public enum LABELS {
		NONE, ALL, CARBON, HYDROGEN
	};

	public enum NUMBERING {
		CANONICAL, SEQUENTIAL
	};

	public enum AROMATICITY {
		SHOW_RINGS, SHOW_KEKULE
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

		String[][] numberLabelsAndValues = new String[][] { { "No Atoms", LABELS.NONE.name() },
				{ "All Atoms", LABELS.ALL.name() }, { "Carbon Atoms", LABELS.CARBON.name() },
				{ "Hydrogen Atoms", LABELS.HYDROGEN.name() } };

		RadioGroupFieldEditor showNumbers = new RadioGroupFieldEditor(CDKPreferenceInitializer.SHOW_NUMBERS,
				"Show atom numbers for: ", 1, numberLabelsAndValues, parent);

		String[][] numberingLabelsAndValues = new String[][] { { "Canonical", NUMBERING.CANONICAL.name() },
				{ "Sequential", NUMBERING.SEQUENTIAL.name() } };

		RadioGroupFieldEditor numbering = new RadioGroupFieldEditor(CDKPreferenceInitializer.NUMBERING_TYPE,
				"Atom number type: ", 1, numberingLabelsAndValues, parent);

		String[][] aromaticityLabelsAndValues = new String[][] { { "Aromatic Form", AROMATICITY.SHOW_RINGS.name() },
				{ "Kekule Form", AROMATICITY.SHOW_KEKULE.name() } };

		RadioGroupFieldEditor showAromaticity = new RadioGroupFieldEditor(CDKPreferenceInitializer.SHOW_AROMATICITY,
				"Aromaticity: ", 1, aromaticityLabelsAndValues, parent);

		addField(showNumbers);
		addField(numbering);
		addField(showAromaticity);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init(final IWorkbench workbench) {
		// nothing to do
	}
}
