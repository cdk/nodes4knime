package org.openscience.cdk.knime.atomsignature;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

public class AtomSignatureSettings {

	private String m_molColumnName;

	public enum SignatureTypes {
		Hose, AtomSignatures
	}

	public enum AtomTypes {
		H, C
	}

	private SignatureTypes m_signatureType = SignatureTypes.AtomSignatures;
	private AtomTypes m_atomType = AtomTypes.C;

	private boolean m_heightSet = false;
	private int m_maxHeight;
	private int m_minHeight;

	/**
	 * Returns the name of the column containing the molecules.
	 * 
	 * @return the molecules' column name
	 */
	public String molColumnName() {

		return m_molColumnName;
	}

	/**
	 * get the minimal height for the fingerprint
	 * 
	 * @return min height
	 */
	public int getMinHeight() {

		return m_minHeight;
	}

	/**
	 * get the max height for the signature
	 * 
	 * @return
	 */
	public int getMaxHeight() {

		return m_maxHeight;
	}

	public boolean isHeightSet() {

		return m_heightSet;
	}

	/**
	 * Returns the type of fingerprints that should be generated.
	 * 
	 * @return the type of fingerprint
	 */
	public SignatureTypes signatureType() {

		return m_signatureType;
	}

	/**
	 * Returns the atom type of interest either protons or carbons.
	 * 
	 * @return the type of atom
	 */
	public AtomTypes atomType() {

		return m_atomType;
	}

	public void setAtomType(final AtomTypes type) {

		m_atomType = type;
	}

	public void setSignatureType(final SignatureTypes type) {

		m_signatureType = type;
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
	 * set the min height of the signature
	 * 
	 * @param mHeight
	 */
	public void minHeight(final int min_height) {

		m_minHeight = min_height;
	}

	/**
	 * set the max height for the signature
	 * 
	 * @param max_height
	 */
	public void maxHeight(final int max_height) {

		m_maxHeight = max_height;
	}

	/**
	 * defined heights
	 * 
	 * @param height_set
	 */
	public void heightSet(final boolean height_set) {

		m_heightSet = height_set;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumn", m_molColumnName);
		settings.addInt("minHeight", m_minHeight);
		settings.addInt("maxHeight", m_maxHeight);
		settings.addBoolean("heightSet", m_heightSet);
		settings.addString("atomType", m_atomType.toString());
		settings.addString("signatureType", m_signatureType.toString());

	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_molColumnName = settings.getString("molColumn");
		m_minHeight = settings.getInt("minHeight");
		m_maxHeight = settings.getInt("maxHeight");
		m_heightSet = settings.getBoolean("heightSet");
		m_atomType = AtomTypes.valueOf(settings.getString("atomType"));
		m_signatureType = SignatureTypes.valueOf(settings.getString("signatureType"));
	}
}
