/* Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- * 
 */
package org.openscience.cdk.knime.elementfilter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
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
 * This is the model implementation of ElementFilter. Filters molecules by a set
 * of defined elements.
 * 
 * @author Stephan Beisken
 */
public class ElementFilterNodeModel extends NodeModel {

	private ElementFilterSettings settings = new ElementFilterSettings();

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
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		String[] elements = settings.getElements().split(",");
		Set<String> elementSet = new HashSet<String>();
		for (String element : elements) {
			elementSet.add(element);
		}

		DataTableSpec inSpec = inData[0].getDataTableSpec();
		final int colIndex = inSpec.findColumnIndex(settings.getMolColumnName());

		BufferedDataContainer filteredTable = exec.createDataContainer(inSpec);
		BufferedDataContainer filteredOutTable = exec.createDataContainer(inSpec);
		
		for (DataRow row : inData[0]) {

			DataCell cell = row.getCell(colIndex);
			if (cell.isMissing()) {
				continue;
			}
			boolean isValid = true;
			IAtomContainer compound = ((CDKValue) cell).getAtomContainer();
			IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(compound);
			List<IElement> sumElements = MolecularFormulaManipulator.getHeavyElements(formula);
			for (IElement element : sumElements) {
				String symbol = element.getSymbol();
				if (!elementSet.contains(symbol)) {
					filteredOutTable.addRowToTable(row);
					isValid = false;
					break;
				}
			}
			if (isValid) {
				filteredTable.addRowToTable(row);
			}
		}
		filteredTable.close();
		filteredOutTable.close();
		return new BufferedDataTable[] { filteredTable.getTable(), filteredOutTable.getTable() };
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
