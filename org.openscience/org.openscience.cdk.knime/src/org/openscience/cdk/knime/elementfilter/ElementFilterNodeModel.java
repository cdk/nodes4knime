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
package org.openscience.cdk.knime.elementfilter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * This is the model implementation of ElementFilter. Filters molecules by a set of defined elements.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ElementFilterNodeModel extends ThreadedTableBuilderNodeModel {

	private ElementFilterSettings settings = new ElementFilterSettings();
	private int colIndex;
	private Set<String> elementSet;

	/**
	 * Constructor for the node model.
	 */
	protected ElementFilterNodeModel() {

		super(1, 2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		String[] elements = settings.getElements().split(",");
		elementSet = new HashSet<String>();
		for (String element : elements) {
			elementSet.add(element);
		}

		colIndex = data[0].getDataTableSpec().findColumnIndex(settings.getMolColumnName());

		return new DataTableSpec[] { data[0].getDataTableSpec(), data[0].getDataTableSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		DataCell cell = inRow.getCell(colIndex);
		if (cell.isMissing()) {
			return;
		}
		boolean isValid = true;
		IAtomContainer compound = ((CDKValue) cell).getAtomContainer();
		IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(compound);
		List<IElement> sumElements = MolecularFormulaManipulator.getHeavyElements(formula);
		for (IElement element : sumElements) {
			String symbol = element.getSymbol();
			if (!elementSet.contains(symbol)) {
				outputTables[1].addRowToTable(inRow);
				isValid = false;
				break;
			}
		}
		if (isValid) {
			outputTables[0].addRowToTable(inRow);
		}
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

		int molCol = inSpecs[0].findColumnIndex(settings.getMolColumnName());
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (molCol >= 0) {
						molCol = -1;
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
					}
				}
			}

			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				settings.setMolColumnName(name);
			}
		}
		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + settings.getMolColumnName() + "' does not exist");
		}

		return new DataTableSpec[] { inSpecs[0], inSpecs[0] };
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

		ElementFilterSettings tmpSettings = new ElementFilterSettings();
		tmpSettings.loadSettings(settings);
		if ((tmpSettings.getMolColumnName() == null) || (tmpSettings.getMolColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}

		try {
			String[] elements = tmpSettings.getElements().split(",");
			IsotopeFactory factory = IsotopeFactory.getInstance(DefaultChemObjectBuilder.getInstance());
			for (String element : elements) {
				factory.getElement(element);
			}
		} catch (Exception exception) {
			throw new InvalidSettingsException("Element string invalid");
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
