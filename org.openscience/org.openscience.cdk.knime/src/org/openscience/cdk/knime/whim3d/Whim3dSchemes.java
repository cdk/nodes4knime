package org.openscience.cdk.knime.whim3d;

/**
 * Constants for the weighting schemes used by the WHIM descriptor.
 * 
 * @author Stephan Beisken
 */
public enum Whim3dSchemes {

	UNITY_WEIGHTS("Unit Weights", "unity"), 
	ATOMIC_MASSES("Atomic Masses", "mass"), 
	ATOMIC_POLARIZABILITIES("Atomic Polarizabilities", "polar"), 
	VdW_VOLUMES("VdW Volumes", "volume"), 
	ATOMIC_ELECTRONEGATIVITIES("Atomic Electronegativities", "eneg");

	private String title;
	private String parameterName;

	private Whim3dSchemes(String title, String parameterName) {

		this.title = title;
		this.parameterName = parameterName;
	}

	/**
	 * @return the column title for the weighing scheme
	 */
	public synchronized String getTitle() {

		return title;
	}

	/**
	 * @return the name of the parameter passed to the descriptor
	 */
	public synchronized String getParameterName() {

		return parameterName;
	}
}
