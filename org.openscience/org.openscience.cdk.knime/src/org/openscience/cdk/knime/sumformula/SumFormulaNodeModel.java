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
package org.openscience.cdk.knime.sumformula;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of SumFormula. Node to generate probable
 * molecular formulas based on a given mass input.
 * 
 * @author Stephan Beisken
 */
public class SumFormulaNodeModel extends NodeModel {

	// private static final String[] RULES = new String[] { "ValidSum",
	// "Elements", "MMElements", "Nitrogen", "RDBE" };

	private SumFormulaSettings settings = new SumFormulaSettings();

	/**
	 * Constructor for the node model.
	 */
	protected SumFormulaNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		DataTableSpec inSpec = inData[0].getDataTableSpec();
		ColumnRearranger rearranger = createColumnRearranger(inSpec);
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0], rearranger, exec);

		return new BufferedDataTable[] { outTable };
	}

	private ColumnRearranger createColumnRearranger(DataTableSpec spec) throws InvalidSettingsException {

		final int colIndex = spec.findColumnIndex(settings.getMassColumn());

		DataColumnSpec[] columnSpecs = new DataColumnSpec[2];
		columnSpecs[0] = new DataColumnSpecCreator("Sum Formula", ListCell.getCollectionType(StringCell.TYPE))
				.createSpec();
		columnSpecs[1] = new DataColumnSpecCreator("Valid Sum", ListCell.getCollectionType(DoubleCell.TYPE))
				.createSpec();

		// for (int i = 1; i < 6; i++) {
		// columnSpecs[i] = new DataColumnSpecCreator(RULES[i - 1],
		// ListCell.getCollectionType(DoubleCell.TYPE))
		// .createSpec();
		// }

		SumFormulaGenerator generator = new SumFormulaGenerator(columnSpecs, colIndex, settings.isExcludeByValidSum());
		ColumnRearranger arrange = new ColumnRearranger(spec);
		arrange.append(generator);

		return arrange;
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

		DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();

		return new DataTableSpec[] { outSpec };
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
