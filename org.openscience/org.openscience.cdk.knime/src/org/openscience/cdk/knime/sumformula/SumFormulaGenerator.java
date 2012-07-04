/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.sumformula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.formula.MassToFormulaTool;
import org.openscience.cdk.formula.MolecularFormulaChecker;
import org.openscience.cdk.formula.rules.ElementRule;
import org.openscience.cdk.formula.rules.IRule;
import org.openscience.cdk.formula.rules.MMElementRule;
import org.openscience.cdk.formula.rules.NitrogenRule;
import org.openscience.cdk.formula.rules.RDBERule;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IMolecularFormulaSet;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Cell factory generating and evaluating molecular formulas based on a given mass.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SumFormulaGenerator implements CellFactory {

	private final DataColumnSpec[] dataColumnSpec;
	private final int massColIndex;
	private final boolean exclude;
	private final MassToFormulaTool mtft;
	private final MolecularFormulaChecker mfc;

	/**
	 * Constructs the cells factory.
	 * 
	 * @param dataColumnSpec data column specifications of the columns to be appended
	 * @param massColIndex column index of the mass containing column
	 * @param exclude boolean if unvalidated formulas should be included in the output
	 */
	public SumFormulaGenerator(DataColumnSpec[] dataColumnSpec, int massColIndex, boolean exclude) {

		this.dataColumnSpec = dataColumnSpec;
		this.massColIndex = massColIndex;
		this.exclude = exclude;

		mtft = new MassToFormulaTool(DefaultChemObjectBuilder.getInstance());

		List<IRule> rules = new ArrayList<IRule>();

		IRule elementRule = new ElementRule();
		IRule mmElementRule = new MMElementRule();
		IRule nitrogenRule = new NitrogenRule();
		IRule rdbeRule = new RDBERule();

		rules.add(elementRule);
		rules.add(mmElementRule);
		rules.add(nitrogenRule);
		rules.add(rdbeRule);

		mfc = new MolecularFormulaChecker(rules);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(DataRow row) {

		DataCell massCell = row.getCell(massColIndex);
		DataCell[] newCells = new DataCell[dataColumnSpec.length];
		if (massCell.isMissing()) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}
		if (!(massCell instanceof DoubleValue)) {
			throw new IllegalArgumentException("No Double cell at " + massColIndex + ": "
					+ massCell.getClass().getName());
		}

		double mass = ((DoubleValue) row.getCell(massColIndex)).getDoubleValue();
		IMolecularFormulaSet mfSet = null;
		mfSet = mtft.generate(mass);

		if (mfSet == null || mfSet.size() == 0) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}

		Collection<StringCell> hillStrings = new ArrayList<StringCell>();
		Collection<DoubleCell> sumDoubles = new ArrayList<DoubleCell>();
		for (IMolecularFormula formula : mfSet.molecularFormulas()) {

			double validSum = mfc.isValidSum(formula);
			if (exclude && validSum != 1) {
				continue;
			}
			hillStrings.add(new StringCell(MolecularFormulaManipulator.getHillString(formula)));
			sumDoubles.add(new DoubleCell(validSum));
		}

		newCells[0] = CollectionCellFactory.createListCell(hillStrings);
		newCells[1] = CollectionCellFactory.createListCell(sumDoubles);

		return newCells;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return dataColumnSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {

		exec.setProgress(curRowNr / (double) rowCount, "Retrieved conversions for row " + curRowNr + " (\"" + lastKey
				+ "\")");
	}
}
