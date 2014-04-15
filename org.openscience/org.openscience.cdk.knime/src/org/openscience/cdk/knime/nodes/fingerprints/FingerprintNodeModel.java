/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
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
package org.openscience.cdk.knime.nodes.fingerprints;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;

/**
 * This is the model for the fingerprint node. It uses the CDK to create
 * fingerprints (which are essentially bit sets) for the molecules in the input
 * table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 * 
 */
public class FingerprintNodeModel extends CDKAdapterNodeModel {

	/**
	 * Creates a new model for the fingerprint node.
	 */
	public FingerprintNodeModel() {
		super(1, 1, new FingerprintSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		FingerprintSettings tmpSettings = new FingerprintSettings();
		tmpSettings.loadSettings(settings);

		if ((tmpSettings.targetColumn() == null) || (tmpSettings.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No compatible molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTable = exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec()));

		FingerprintWorker worker = new FingerprintWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec.createSubProgress(1),
				convertedTables[0].getRowCount(), outputTable, settings(FingerprintSettings.class));

		// hack for linear fingerprints:
		// "too many paths generate. We're working making this faster but for now try generating paths with a smaller length"
		worker.setModel(this);
		
		try {
			worker.run(convertedTables[0]);
		} finally {
			outputTable.close();
		}

		return new BufferedDataTable[] { outputTable.getTable() };
	}

	private DataTableSpec appendSpec(DataTableSpec spec) {

		String newColName = settings(FingerprintSettings.class).fingerprintType() + " fingerprints for "
				+ settings.targetColumn();
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);

		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DenseBitVectorCell.TYPE);
		DataColumnSpec appendSpec = c.createSpec();

		return new DataTableSpec(spec, new DataTableSpec(appendSpec));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];

		return new DataTableSpec[] { appendSpec(outSpec) };
	}
}
