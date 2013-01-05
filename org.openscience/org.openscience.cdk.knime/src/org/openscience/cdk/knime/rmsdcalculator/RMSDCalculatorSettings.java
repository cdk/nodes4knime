/*
 * Copyright (c) 2013, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.rmsdcalculator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class RMSDCalculatorSettings {
	// always set m_molColumnName to null...
	private String m_molColumnName = null;
	
	public enum AlignmentTypes {
        Kabsch, Isomorphic
	}
	
	private AlignmentTypes m_alignmentType = AlignmentTypes.Kabsch;
	
	/**
     * Loads the settings from the given node settings object.
     *
     * @param settings node settings
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_molColumnName = settings.getString("molColumn", null);
        m_alignmentType =
                AlignmentTypes.valueOf(settings.getString("alignmentType",
                        AlignmentTypes.Kabsch.toString()));
    }
    
	public void loadSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		m_molColumnName = settings.getString("molColumn");
		m_alignmentType =
            AlignmentTypes.valueOf(settings.getString("alignmentType"));
	}

	public String molColumnName() {
		return m_molColumnName;
	}
	
	/**
     * Returns the type of alignment that should be used
     *
     * @return the type of alignment
     */
    public AlignmentTypes alignmentType() {
        return m_alignmentType;
    }
    

	public void molColumnName(final String selectedColumn) {
		m_molColumnName = selectedColumn;		
	}

	public void saveSettings(NodeSettingsWO settings) {
		settings.addString("molColumn", m_molColumnName);
		settings.addString("alignmentType", m_alignmentType.toString());
	}
	
	/**
     * Sets the type of alignment that should be used.
     *
     * @param type the type of alignment
     */
    public void alignmentType(final AlignmentTypes type) {
        m_alignmentType = type;
    }


	
	
	

}
