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
package org.openscience.cdk.knime.sumformula;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
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
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of SumFormula. Node to generate probable molecular formulas based on a given mass
 * input.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SumFormulaNodeModel extends ThreadedColAppenderNodeModel {

	private SumFormulaSettings settings = new SumFormulaSettings();

	/**
	 * Constructor for the node model.
	 */
	protected SumFormulaNodeModel() {

		super(1, 1);
		
		this.setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(settings.getMassColumn());
		final MassToFormulaTool mtft = new MassToFormulaTool(DefaultChemObjectBuilder.getInstance());
		final MolecularFormulaChecker mfc = new MolecularFormulaChecker(getRules());

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell massCell = row.getCell(colIndex);
				DataCell[] newCells = new DataCell[2];
				if (massCell.isMissing()) {
					Arrays.fill(newCells, DataType.getMissingCell());
					return newCells;
				}
				if (!(massCell instanceof DoubleValue)) {
					throw new IllegalArgumentException("No Double cell at " + colIndex + ": "
							+ massCell.getClass().getName());
				}

				double mass = ((DoubleValue) row.getCell(colIndex)).getDoubleValue();
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
					if (settings.isExcludeByValidSum() && validSum != 1) {
						continue;
					}
					hillStrings.add(new StringCell(MolecularFormulaManipulator.getString(formula)));
					sumDoubles.add(new DoubleCell(validSum));
				}

				newCells[0] = CollectionCellFactory.createListCell(hillStrings);
				newCells[1] = CollectionCellFactory.createListCell(sumDoubles);

				return newCells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				DataColumnSpecCreator crea1 = new DataColumnSpecCreator("Sum Formula", ListCell.getCollectionType(StringCell.TYPE));
				DataColumnSpecCreator crea2 = new DataColumnSpecCreator("Valid Sum", ListCell.getCollectionType(DoubleCell.TYPE));
				return new DataColumnSpec[] { crea1.createSpec(), crea2.createSpec() };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
	
	private List<IRule> getRules() {
		
		List<IRule> rules = new ArrayList<IRule>();

		IRule elementRule = new ElementRule();
		IRule mmElementRule = new MMElementRule();
		IRule nitrogenRule = new NitrogenRule();
		IRule rdbeRule = new RDBERule();

		rules.add(elementRule);
		rules.add(mmElementRule);
		rules.add(nitrogenRule);
		rules.add(rdbeRule);
		
		return rules;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int stringColumn = inSpecs[0].findColumnIndex(settings.getMassColumn());
		if (stringColumn == -1) {
			throw new InvalidSettingsException("Mass column '" + settings.getMassColumn() + "' does not exist");
		}

		DataColumnSpecCreator crea1 = new DataColumnSpecCreator("Sum Formula", ListCell.getCollectionType(StringCell.TYPE));
		DataColumnSpecCreator crea2 = new DataColumnSpecCreator("Valid Sum", ListCell.getCollectionType(DoubleCell.TYPE));
		
		DataTableSpec outDataTableSpec = new DataTableSpec(crea1.createSpec(), crea2.createSpec());
		
		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], outDataTableSpec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		this.settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		this.settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SumFormulaSettings tmpSettings = new SumFormulaSettings();
		tmpSettings.loadSettings(settings);
		if ((tmpSettings.getMassColumn() == null) || (tmpSettings.getMassColumn().length() == 0)) {
			throw new InvalidSettingsException("No mass column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

}
