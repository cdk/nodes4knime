/*
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 */
package org.openscience.cdk.knime;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.ext.chem.moss.CDKMolConverter;
import org.knime.ext.chem.moss.MolConverter;
import org.openscience.cdk.knime.CDKPreferencePage.LABELS;
import org.openscience.cdk.knime.CDKPreferencePage.NUMBERING;
import org.osgi.framework.BundleContext;

/**
 * This is the eclipse bundle activator. Note: KNIME node vendors probably won't have to do anything here, as this class
 * is only needed by the eclipse platform/plugin mechanism.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class CDKNodePlugin extends AbstractUIPlugin {

	/** Make sure that this *always* matches the ID in plugin.xml. */
	public static final String PLUGIN_ID = "org.knime.ext.chem.cdk";

	// The shared instance.
	private static CDKNodePlugin plugin;

	private static LABELS showNumbers = LABELS.NONE;
	private static NUMBERING numbering = NUMBERING.CANONICAL;

	/**
	 * The constructor.
	 */
	public CDKNodePlugin() {

		super();
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation.
	 * 
	 * @param context the OSGI bundle context
	 * @throws Exception if this plugin could not be started
	 */
	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);
		/*
		 * The following line will register the CDK renderer as one standard Smiles renderer. If it is provided with the
		 * smiles string, it will use the CDK parser (which is really really slow!!!) to instantiate a CDK molecule on
		 * the fly, which is then rendered.
		 * 
		 * I comment out this line because the "online"-rendering is slow. If someone insists to have the CDK renderer
		 * he must use the CDK parser to create the molecules first.
		 */
		// SmilesValue.UTILITY.addRenderer(new CDKValueRenderer());

		final IPreferenceStore pStore = getDefault().getPreferenceStore();
		pStore.addPropertyChangeListener(new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				if (event.getProperty().equals(CDKPreferenceInitializer.SHOW_NUMBERS)) {
					showNumbers = LABELS.valueOf(pStore.getString(CDKPreferenceInitializer.SHOW_NUMBERS));
				} else if (event.getProperty().equals(CDKPreferenceInitializer.NUMBERING_TYPE)) {
					numbering = NUMBERING.valueOf(pStore.getString(CDKPreferenceInitializer.NUMBERING_TYPE));
				}
			}
		});

		showNumbers = LABELS.valueOf(pStore.getString(CDKPreferenceInitializer.SHOW_NUMBERS));
		numbering = NUMBERING.valueOf(pStore.getString(CDKPreferenceInitializer.NUMBERING_TYPE));

		try {
			// may fail if MoSS is not installed
			MolConverter.register(CDKMolConverter.class);
		} catch (NoClassDefFoundError err) {
			// ignore it
		}
	}

	/**
	 * This method is called when the plug-in is stopped.
	 * 
	 * @param context The OSGI bundle context
	 * @throws Exception If this plugin could not be stopped
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {

		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return Singleton instance of the Plugin
	 */
	public static CDKNodePlugin getDefault() {

		return plugin;
	}

	/**
	 * Returns which atom id labels should be shown.
	 * 
	 * @return <code>true</code> if the id labels should be displayed, <code>false</code> otherwise
	 */
	public static LABELS showNumbers() {

		return showNumbers;
	}

	/**
	 * Returns the numbering type that should be used for the atom ids.
	 * 
	 * @return the numbering type
	 */
	public static NUMBERING numbering() {

		return numbering;
	}
}
