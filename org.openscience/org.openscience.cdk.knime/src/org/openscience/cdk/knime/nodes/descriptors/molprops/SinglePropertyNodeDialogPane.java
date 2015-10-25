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
package org.openscience.cdk.knime.nodes.descriptors.molprops;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.knime.commons.CDKNodeUtils;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class SinglePropertyNodeDialogPane extends DefaultNodeSettingsPane {

	/**
	 * Creates new pane with a single column selection combo box.
	 * 
	 * @see DefaultNodeSettingsPane#DefaultNodeSettingsPane()
	 */
	@SuppressWarnings("unchecked")
	public SinglePropertyNodeDialogPane() {

		SettingsModelString model = SinglePropertyNodeModel.createColSelectorSettingsModel();
		addDialogComponent(new DialogComponentColumnNameSelection(model, "Molecule column", 0, true, CDKNodeUtils.ACCEPTED_VALUE_CLASSES));
	}
}
