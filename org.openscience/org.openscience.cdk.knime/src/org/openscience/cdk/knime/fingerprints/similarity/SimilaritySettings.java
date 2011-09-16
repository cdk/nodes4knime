package org.openscience.cdk.knime.fingerprints.similarity;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the similarity node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilaritySettings {

	/** Enum for the different aggregation methods. */
	public enum AggregationMethod {
		Minimum, Maximum, Average
	}
	public enum FingerprintTypes {
        Standard, Extended, EState, MACCS, Pubchem
    }

	private String m_fingerprintColumn = null;
	private String m_fingerprintRefColumn = null;
	private AggregationMethod m_aggregation = AggregationMethod.Average;
	private FingerprintTypes m_fingerprintType = FingerprintTypes.MACCS;

	/**
	 * Returns the name of the column that holds the fingerprints.
	 * 
	 * @return a column name
	 */
	public String fingerprintColumn() {
		return m_fingerprintColumn;
	}

	/**
	 * Sets the name of the column that holds the fingerprints.
	 * 
	 * @param columnName a column name
	 */
	public void fingerprintColumn(final String columnName) {
		m_fingerprintColumn = columnName;
	}
	
	/**
     * Returns the type of fingerprints that should be used.
     *
     * @return the type of fingerprint
     */
    public FingerprintTypes fingerprintType() {
        return m_fingerprintType;
    }

    /**
     * Sets the type of fingerprints that should be used.
     *
     * @param type the type of fingerprint
     */
    public void fingerprintType(final FingerprintTypes type) {
        m_fingerprintType = type;
    }

	/**
	 * Returns the name of the column that holds the reference fingerprints.
	 * 
	 * @return a column name
	 */
	public String fingerprintRefColumn() {
		return m_fingerprintRefColumn;
	}

	/**
	 * Sets the name of the column that holds the reference fingerprints.
	 * 
	 * @param columnName a column name
	 */
	public void fingerprintRefColumn(final String columnName) {
		m_fingerprintRefColumn = columnName;
	}

	/**
	 * Returns the aggregation method that should be used.
	 * 
	 * @return the aggregation method
	 */
	public AggregationMethod aggregationMethod() {
		return m_aggregation;
	}

	/**
	 * Sets the aggregation method that should be used.
	 * 
	 * @param type the aggregation method
	 */
	public void aggregationMethod(final AggregationMethod aggregation) {
		m_aggregation = aggregation;
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void loadSettingsForDialog(final NodeSettingsRO settings) {
		m_fingerprintColumn = settings.getString("molColumn", null);
		m_fingerprintRefColumn = settings.getString("molRefColumn", null);
		m_aggregation = AggregationMethod.valueOf(settings.getString("aggregationMethod",
				AggregationMethod.Average.toString()));
		m_fingerprintType = FingerprintTypes.valueOf(settings.getString("fingerprintType",
				FingerprintTypes.MACCS.toString()));
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings node settings
	 * @throws InvalidSettingsException if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_fingerprintColumn = settings.getString("molColumn");
		m_fingerprintRefColumn = settings.getString("molRefColumn");
		m_aggregation = AggregationMethod.valueOf(settings.getString("aggregationMethod"));
		m_fingerprintType = FingerprintTypes.valueOf(settings.getString("fingerprintType"));
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings node settings
	 */
	public void saveSettingsTo(final NodeSettingsWO settings) {
		settings.addString("molColumn", m_fingerprintColumn);
		settings.addString("molRefColumn", m_fingerprintRefColumn);
		settings.addString("aggregationMethod", m_aggregation.toString());
		settings.addString("fingerprintType", m_fingerprintType.toString());
	}
}
