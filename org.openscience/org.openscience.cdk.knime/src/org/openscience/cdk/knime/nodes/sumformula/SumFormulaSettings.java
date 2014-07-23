/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.sumformula;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * Settings for the "SumFormulaNode" Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 * 
 */
public class SumFormulaSettings implements CDKSettings {

	private String massColumn;
	private String elements = "C,H,N,O";
	private int[] cRange = new int[] { 0, 30 };
	private boolean incAll = false;
	private boolean incSpec = true;
	private double tolerance = 0.5;
	private boolean applyNitrogenRule = true;
	private String applyRatioRule = "HSiNOPSBrClF/C-Common Range";
	private String applyNumberRule = "Wiley-500";
	
	protected String[] listElements = new String[]{
		    "C", "H", "O", "N", "Si", "P", "S", "F", "Cl",
		    "Br", "I", "Sn", "B", "Pb", "Tl", "Ba", "In", "Pd",
		    "Pt", "Os", "Ag", "Zr", "Se", "Zn", "Cu", "Ni", "Co", 
		    "Fe", "Cr", "Ti", "Ca", "K", "Al", "Mg", "Na", "Ce",
		    "Hg", "Au", "Ir", "Re", "W", "Ta", "Hf", "Lu", "Yb", 
		    "Tm", "Er", "Ho", "Dy", "Tb", "Gd", "Eu", "Sm", "Pm",
		    "Nd", "Pr", "La", "Cs", "Xe", "Te", "Sb", "Cd", "Rh", 
		    "Ru", "Tc", "Mo", "Nb", "Y", "Sr", "Rb", "Kr", "As", 
		    "Ge", "Ga", "Mn", "V", "Sc", "Ar", "Ne", "Be", "Li", 
		    "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", 
		    "Th", "Pa", "U", "Np", "Pu"};

	/**
	 * Gets the name of the mass containing double column.
	 * 
	 * @return the massColumn
	 */
	public String targetColumn() {
		return massColumn;
	}

	/**
	 * Sets the name of the mass containing double column.
	 * 
	 * @param massColumn the massColumn to set
	 */
	public void targetColumn(final String massColumn) {
		this.massColumn = massColumn;
	}

	public final boolean isApplyNitrogenRule() {
		return applyNitrogenRule;
	}

	public final void setApplyNitrogenRule(final boolean applyNitrogenRule) {
		this.applyNitrogenRule = applyNitrogenRule;
	}

	public final String isApplyRatioRule() {
		return applyRatioRule;
	}

	public final void setApplyRatioRule(final String applyRatioRule) {
		this.applyRatioRule = applyRatioRule;
	}

	public final String isApplyNumberRule() {
		return applyNumberRule;
	}

	public final void setApplyNumberRule(final String applyNumberRule) {
		this.applyNumberRule = applyNumberRule;
	}
	
	public final int[] getcRange() {
		return cRange;
	}

	public final void setcRange(int[] cRange) {
		this.cRange = cRange;
	}

	/**
	 * Saves the settings into the given node settings object.
	 * 
	 * @param settings a node settings object
	 */
	public void saveSettings(final NodeSettingsWO settings) {

		settings.addString("massColumn", massColumn);
		settings.addString("elements", elements);
		settings.addBoolean("incAll", incAll);
		settings.addBoolean("incSpec", incSpec);
		settings.addDouble("tolerance", tolerance);
		settings.addBoolean("nitrogenRule", applyNitrogenRule);
		settings.addString("ratioRule", applyRatioRule);
		settings.addString("numberRule", applyNumberRule);
		settings.addIntArray("crange", cRange);
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings a node settings object
	 * @throws InvalidSettingsException if not all required settings are available
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		massColumn = settings.getString("massColumn");
		elements = settings.getString("elements");
		incAll = settings.getBoolean("incAll");
		incSpec = settings.getBoolean("incSpec");
		tolerance = settings.getDouble("tolerance");
		applyNitrogenRule = settings.getBoolean("nitrogenRule");
		applyRatioRule = settings.getString("ratioRule");
		applyNumberRule = settings.getString("numberRule");
		cRange = settings.getIntArray("crange");
	}
	
	public String elements() {
		return elements;
	}
	
	public void elements(String elements) {
		this.elements = elements;
	}

	public boolean incAll() {
		return incAll;
	}

	public void incAll(boolean incAll) {
		this.incAll = incAll;
	}

	public boolean incSpec() {
		return incSpec;
	}

	public void incSpec(boolean incSpec) {
		this.incSpec = incSpec;
	}

	public double tolerance() {
		return tolerance;
	}

	public void tolerance(double tolerance) {
		this.tolerance = tolerance;
	}
}
