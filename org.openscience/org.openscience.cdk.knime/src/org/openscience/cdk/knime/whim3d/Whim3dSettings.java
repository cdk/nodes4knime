package org.openscience.cdk.knime.whim3d;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings for the "Whim3d" Node.
 * 
 * @author Stephan Beisken
 * 
 */
public class Whim3dSettings {

	private String molColumnName;

	private boolean schemeUnitWeights;
	private boolean schemeAtomicMasses;
	private boolean schemeVdWVolumes;
	private boolean schemeAtomicElectronneg;
	private boolean schemeAtomicPolariz;

	/**
	 * @return the molecule column name
	 */
	public String getMolColumnName() {

		return molColumnName;
	}

	/**
	 * @param molColumnName the molecule column name
	 */
	public void setMolColumnName(String molColumnName) {

		this.molColumnName = molColumnName;
	}

	/**
	 * @return if weighting scheme: unit weights
	 */
	public boolean isSchemeUnitWeights() {

		return schemeUnitWeights;
	}

	/**
	 * @param schemeUnitWeights if weighting scheme: unit weights
	 */
	public void setSchemeUnitWeights(boolean schemeUnitWeights) {

		this.schemeUnitWeights = schemeUnitWeights;
	}

	/**
	 * @return if weighting scheme: atomic masses
	 */
	public boolean isSchemeAtomicMasses() {

		return schemeAtomicMasses;
	}

	/**
	 * @param schemeAtomicMasses if weighting scheme: atomic masses
	 */
	public void setSchemeAtomicMasses(boolean schemeAtomicMasses) {

		this.schemeAtomicMasses = schemeAtomicMasses;
	}

	/**
	 * @return if weighting scheme: van der Waals volumes
	 */
	public boolean isSchemeVdWVolumes() {

		return schemeVdWVolumes;
	}

	/**
	 * @param schemeVdWVolumes if weighting scheme: van der Waals volumes
	 */
	public void setSchemeVdWVolumes(boolean schemeVdWVolumes) {

		this.schemeVdWVolumes = schemeVdWVolumes;
	}

	/**
	 * @return if weighting scheme: Mulliken atomic electronegativites
	 */
	public boolean isSchemeAtomicElectronneg() {

		return schemeAtomicElectronneg;
	}

	/**
	 * @param schemeAtomicElectronneg if weighting scheme: Mulliken atomic electronegativites
	 */
	public void setSchemeAtomicElectronneg(boolean schemeAtomicElectronneg) {

		this.schemeAtomicElectronneg = schemeAtomicElectronneg;
	}

	/**
	 * @return if weighting scheme: atomic polarizabilities
	 */
	public boolean isSchemeAtomicPolariz() {

		return schemeAtomicPolariz;
	}

	/**
	 * @param schemeAtomicPolariz if weighting scheme: atomic polarizabilities
	 */
	public void setSchemeAtomicPolariz(boolean schemeAtomicPolariz) {

		this.schemeAtomicPolariz = schemeAtomicPolariz;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("molColumnName", molColumnName);
		settings.addBoolean("schemeUnitWeights", schemeUnitWeights);
		settings.addBoolean("schemeAtomicMasses", schemeAtomicMasses);
		settings.addBoolean("schemeVdWVolumes", schemeVdWVolumes);
		settings.addBoolean("schemeAtomicElectronneg", schemeAtomicElectronneg);
		settings.addBoolean("schemeAtomicPolariz", schemeAtomicPolariz);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		molColumnName = settings.getString("molColumnName");
		schemeUnitWeights = settings.getBoolean("schemeUnitWeights");
		schemeAtomicMasses = settings.getBoolean("schemeAtomicMasses");
		schemeVdWVolumes = settings.getBoolean("schemeVdWVolumes");
		schemeAtomicElectronneg = settings.getBoolean("schemeAtomicElectronneg");
		schemeAtomicPolariz = settings.getBoolean("schemeAtomicPolariz");
	}
}
