/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.formula.MassToFormulaTool;
import org.openscience.cdk.formula.MolecularFormulaChecker;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.formula.rules.ChargeRule;
import org.openscience.cdk.formula.rules.ElementRatioRule;
import org.openscience.cdk.formula.rules.ElementRule;
import org.openscience.cdk.formula.rules.IRule;
import org.openscience.cdk.formula.rules.MMElementRule;
import org.openscience.cdk.formula.rules.NitrogenRule;
import org.openscience.cdk.formula.rules.ToleranceRangeRule;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IMolecularFormulaSet;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of SumFormula. Node to generate probable molecular formulas based on a given mass
 * input.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SumFormulaNodeModel extends CDKNodeModel {

	private List<IRule> rules;
	private MolecularFormulaChecker mfc;
	
	/**
	 * Constructor for the node model.
	 */
	protected SumFormulaNodeModel() {
		super(1, 1, new SumFormulaSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		DataColumnSpecCreator crea1 = new DataColumnSpecCreator(
				DataTableSpec.getUniqueColumnName(spec, "Sum Formula"),
				ListCell.getCollectionType(StringCell.TYPE));

		DataColumnSpec[] appendSpec = new DataColumnSpec[] { crea1.createSpec() };
		columnIndex = spec.findColumnIndex(settings.targetColumn());
		
		// CUSTOM PARAMETERS
		rules = new ArrayList<IRule>();
		try {
			// restriction for occurrence elements
			IsotopeFactory ifac = Isotopes.getInstance();
			MolecularFormulaRange mfRange = new MolecularFormulaRange();
			if (settings(SumFormulaSettings.class).incSpec()) {
				String[] els = settings(SumFormulaSettings.class).elements().split(",");
				for (String el : els) {
					if (el.equals("C")) {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).getcRange()[0], 
								settings(SumFormulaSettings.class).getcRange()[1]);
					} else if (el.equals("H")) {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).gethRange()[0], 
								settings(SumFormulaSettings.class).gethRange()[1]);
					} else {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).getoRange()[0], 
								settings(SumFormulaSettings.class).getoRange()[1]);
					}
				}
			} else if (settings(SumFormulaSettings.class).incAll()) {
				for (String el : settings(SumFormulaSettings.class).listElements) {
					if (el.equals("C")) {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).getcRange()[0], 
								settings(SumFormulaSettings.class).getcRange()[1]);
					} else if (el.equals("H")) {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).gethRange()[0], 
								settings(SumFormulaSettings.class).gethRange()[1]);
					} else {
						mfRange.addIsotope(ifac.getMajorIsotope(el), 
								settings(SumFormulaSettings.class).getoRange()[0], 
								settings(SumFormulaSettings.class).getoRange()[1]);
					}
				}
			} else {
				String[] rems = settings(SumFormulaSettings.class).elements().split(",");
				Set<String> remSet = new HashSet<String>(Arrays.asList(rems));
				for (String el : settings(SumFormulaSettings.class).listElements) {
					if (!remSet.contains(el)) {
						if (el.equals("C")) {
							mfRange.addIsotope(ifac.getMajorIsotope(el), 
									settings(SumFormulaSettings.class).getcRange()[0], 
									settings(SumFormulaSettings.class).getcRange()[1]);
						} else if (el.equals("H")) {
							mfRange.addIsotope(ifac.getMajorIsotope(el), 
									settings(SumFormulaSettings.class).gethRange()[0], 
									settings(SumFormulaSettings.class).gethRange()[1]);
						} else {
							mfRange.addIsotope(ifac.getMajorIsotope(el), 
									settings(SumFormulaSettings.class).getoRange()[0], 
									settings(SumFormulaSettings.class).getoRange()[1]);
						}
					}
				}
			}
			IRule rule1  = new ElementRule();
			rule1.setParameters(new Object[] { mfRange });
			rules.add(rule1);
			// occurrence for charge
			IRule rule2  = new ChargeRule(); // default 0.0 neutral
			rules.add(rule2);
			// occurrence for tolerance
			IRule rule3 = new ToleranceRangeRule(); // default 0.05
			rule3.setParameters(new Object[] { 0.0, settings(SumFormulaSettings.class).tolerance() });
			rules.add(rule3);
			// set options
			mfc = new MolecularFormulaChecker(getRules());
		} catch (Exception e) {
			e.printStackTrace();
			setWarningMessage("Rule violation, falling back to default rules.");
		}
		
		AbstractCellFactory cf = new AbstractCellFactory(true, appendSpec) {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell massCell = row.getCell(columnIndex);
				DataCell[] newCells = new DataCell[1];
				if (massCell.isMissing()) {
					newCells[0] = DataType.getMissingCell();
					return newCells;
				}
				if (!(massCell instanceof DoubleValue)) {
					throw new IllegalArgumentException("No Double cell at " + columnIndex + ": "
							+ massCell.getClass().getName());
				}

				MassToFormulaTool mtft = new MassToFormulaTool(DefaultChemObjectBuilder.getInstance());
				if (rules.size() == 3) {
					try {
						mtft.setRestrictions(rules);
					} catch (CDKException e) {
						setWarningMessage("Rule violation, falling back to default rules.");
						mtft.setDefaultRestrictions();
					}
				}
				double mass = ((DoubleValue) row.getCell(columnIndex)).getDoubleValue();
				IMolecularFormulaSet mfSet = null;
				mfSet = mtft.generate(mass);

				if (mfSet == null || mfSet.size() == 0) {
					newCells[0] = DataType.getMissingCell();
					return newCells;
				}
				
				Collection<StringCell> hillStrings = new ArrayList<StringCell>();
				for (IMolecularFormula formula : mfSet.molecularFormulas()) {

					try {
						double validSum = mfc.isValidSum(formula);
						if (validSum != 1) {
							continue;
						}
						hillStrings.add(new StringCell(MolecularFormulaManipulator.getString(formula)));
					} catch (Exception exception) {
						exception.printStackTrace();
						hillStrings.add(new StringCell(MolecularFormulaManipulator.getString(formula)));
					}
				}

				newCells[0] = CollectionCellFactory.createListCell(hillStrings);

				return newCells;
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.append(cf);

		return arranger;
	}

	private List<IRule> getRules() throws CDKException {

		List<IRule> rules = new ArrayList<IRule>();

		if (settings(SumFormulaSettings.class).isApplyNitrogenRule()) {
			IRule nitrogenRule = new NitrogenRule();
			rules.add(nitrogenRule);
		}
		if (!settings(SumFormulaSettings.class).isApplyNumberRule().isEmpty()) {
			IRule mmRule = new MMElementRule();
			Object[] params = new Object[2];
			String[] els = settings(SumFormulaSettings.class).isApplyNumberRule().split("-");
			if (els[0].equals("DNP")) {
				params[0] = MMElementRule.Database.DNP;
			} else {
				params[0] = MMElementRule.Database.WILEY;
			}
			if (els[1].equals("500")) {
				params[1] = MMElementRule.RangeMass.Minus500;
			} else if (els[1].equals("1000")) {
				params[1] = MMElementRule.RangeMass.Minus1000;
			} else if (els[1].equals("2000")) {
				params[1] = MMElementRule.RangeMass.Minus2000;
			} else {
				params[1] = MMElementRule.RangeMass.Minus3000;
			} 
			mmRule.setParameters(params);
			rules.add(mmRule);
		}
		if (!settings(SumFormulaSettings.class).isApplyRatioRule().isEmpty()) {
			Object[] params = new Object[2];
			String[] els = settings(SumFormulaSettings.class).isApplyRatioRule().split("-");
			if (els[1].equals("Common Range")) {
				params[1] = ElementRatioRule.RatioRange.COMMON;
			} else if (els[1].equals("Extended Range")) {
				params[1] = ElementRatioRule.RatioRange.EXTENDED;
			} else {
				params[1] = ElementRatioRule.RatioRange.EXTREME;
			}
			if (els[0].equals("H/C")) {
				params[0] = ElementRatioRule.RatioType.HYDROGEN_CARBON;
			} else if (els[0].equals("SiNOPSBrClF/C")) {
				params[0] = ElementRatioRule.RatioType.HETERATOMS_CARBON;
			} else {
				params[0] = ElementRatioRule.RatioType.ALL;
			}
			IRule ratioRule = new ElementRatioRule(params);
			rules.add(ratioRule);
		}

		return rules;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		settings.targetColumn(CDKNodeUtils.autoConfigure(inSpecs[0], settings.targetColumn(), DoubleValue.class));

		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SumFormulaSettings tmpSettings = new SumFormulaSettings();
		tmpSettings.loadSettings(settings);
		if ((tmpSettings.targetColumn() == null) || (tmpSettings.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No mass column chosen");
		}
	}
}
