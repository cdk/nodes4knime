package org.openscience.cdk.knime.sugarremover;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public class SugarRemoverSettings {
	private String m_molColumnName;

    private boolean m_replaceColumn = true;
    private String m_appendColumnName;
    
    /**
     * Returns the name of the column containing the molecules.
     * 
     * @return the molecules' column name
     */
    public String molColumnName() {
        return m_molColumnName;
    }

    /**
     * Sets the name of the column containing the molecules.
     * 
     * @param colName the molecules' column name
     */
    public void molColumnName(final String colName) {
        m_molColumnName = colName;
    }

    /**
     * Returns if the molecule column should be replaced or if a new column
     * should be appended.
     * 
     * @return <code>true</code> if the column should be replaced,
     *         <code>false</code> if a new column should be appended
     */
    public boolean replaceColumn() {
        return m_replaceColumn;
    }

    /**
     * Sets if the molecule column should be replaced or if a new column should
     * be appended.
     * 
     * @param replace <code>true</code> if the column should be replaced,
     *            <code>false</code> if a new column should be appended
     */
    public void replaceColumn(final boolean replace) {
        m_replaceColumn = replace;
    }

    /**
     * @return the appendColumnName
     */
    public String appendColumnName() {
        return m_appendColumnName;
    }

    /**
     * @param appendColumnName the appendColumnName to set
     */
    public void appendColumnName(final String appendColumnName) {
        m_appendColumnName = appendColumnName;
    }
    
    /**
     * Saves the settings into the given node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("molColumn", m_molColumnName);
        settings.addBoolean("replaceColumn", m_replaceColumn);
        settings.addString("appendColName", appendColumnName());
    }

    /**
     * Loads the settings from the given node settings object.
     * 
     * @param settings a node settings object
     * @throws InvalidSettingsException if not all required settings are
     *             available
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_molColumnName = settings.getString("molColumn");
        m_replaceColumn = settings.getBoolean("replaceColumn");
        m_appendColumnName = settings.getString("appendColName", m_molColumnName + " (light)");
    }
}
