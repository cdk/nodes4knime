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
package org.openscience.cdk.knime.opsin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.base.node.io.tablecreator.prop.SmilesTypeHelper;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.type.CDKCell;

/**
 * This is the model implementation of OpsinNameConverter. Converts IUPAC names
 * into chemical structures.
 * 
 * @author Stephan Beisken
 */
public class OpsinNameConverterNodeModel extends NodeModel {

	private OpsinNameConverterSettings settings = new OpsinNameConverterSettings();

	/**
	 * Constructor for the node model.
	 */
	protected OpsinNameConverterNodeModel() {

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

		// get user configuration and collect corresponding webservice suffices
		List<DataColumnSpec> dataColumnSpecs = new ArrayList<DataColumnSpec>();
		List<String> urlSuffix = new ArrayList<String>();
		if (settings.isAddCdk()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator("CDK", CDKCell.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("cdk");
		}
		if (settings.isAddCml()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator("CML", XMLCell.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("cml");
		}
		if (settings.isAddInChI()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator("InChI", StringCell.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("inchi");
		}
		if (settings.isAddPng()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator("Png", DataType.getType(PNGImageCell.class))
					.createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("png");
		}
		if (settings.isAddSmiles()) {
			SmilesTypeHelper smilesTypeHelper = SmilesTypeHelper.INSTANCE;
			DataColumnSpec colSpec = new DataColumnSpecCreator("SMILES", smilesTypeHelper.getSmilesType()).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("smi");
		}

		final int colIndex = spec.findColumnIndex(settings.getNameColumn());
		// create the OPSIN calling data cell factory
		OpsinNameConverterGenerator generator = new OpsinNameConverterGenerator(colIndex, urlSuffix,
				dataColumnSpecs.toArray(new DataColumnSpec[] {}));
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

		int stringColumn = inSpecs[0].findColumnIndex(settings.getNameColumn());
		if (stringColumn == -1) {
			throw new InvalidSettingsException("String column '" + settings.getNameColumn() + "' does not exist");
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

		OpsinNameConverterSettings tmpSettings = new OpsinNameConverterSettings();
		tmpSettings.loadSettings(settings);
		if ((tmpSettings.getNameColumn() == null) || (tmpSettings.getNameColumn().length() == 0)) {
			throw new InvalidSettingsException("No string column chosen");
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
