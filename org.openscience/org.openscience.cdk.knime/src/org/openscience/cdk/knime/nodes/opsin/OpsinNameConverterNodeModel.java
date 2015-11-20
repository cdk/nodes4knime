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
package org.openscience.cdk.knime.nodes.opsin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.chem.types.SmilesCell;
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
import org.openscience.cdk.knime.type.CDKCell3;

/**
 * This is the model implementation of OpsinNameConverter. Converts IUPAC names
 * into chemical structures.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
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
			DataColumnSpec colSpec = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, "CDK"),
					CDKCell3.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("cdk");
		}
		if (settings.isAddCml()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, "CML"),
					XMLCell.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("cml");
		}
		if (settings.isAddInChI()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, "InChI"),
					StringCell.TYPE).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("inchi");
		}
		if (settings.isAddPng()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, "Png"),
					DataType.getType(PNGImageCell.class)).createSpec();
			dataColumnSpecs.add(colSpec);
			urlSuffix.add("png");
		}
		if (settings.isAddSmiles()) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(spec, "SMILES"),
					SmilesCell.TYPE).createSpec();
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
