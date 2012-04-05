package org.openscience.cdk.knime.rmsdcalculator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

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
