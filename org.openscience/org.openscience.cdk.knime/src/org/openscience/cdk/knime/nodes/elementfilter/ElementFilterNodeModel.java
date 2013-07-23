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
package org.openscience.cdk.knime.nodes.elementfilter;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;

/**
 * This is the model implementation of ElementFilter. Filters molecules by a set of defined elements.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ElementFilterNodeModel extends CDKAdapterNodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected ElementFilterNodeModel() {
		super(1, 2, new ElementFilterSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTableMatched = exec.createDataContainer(convertedTables[0].getDataTableSpec());
		BufferedDataContainer outputTableMissed = exec.createDataContainer(convertedTables[0].getDataTableSpec());

		ElementFilterWorker worker = new ElementFilterWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec,
				settings(ElementFilterSettings.class), outputTableMatched, outputTableMissed);

		try {
			worker.run(convertedTables[0]);
		} catch (InterruptedException e) {
			CanceledExecutionException cee = new CanceledExecutionException(e.getMessage());
			cee.initCause(e);
			throw cee;
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause == null) {
				cause = e;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			throw new RuntimeException(cause);
		} finally {
			outputTableMatched.close();
			outputTableMissed.close();
		}

		return new BufferedDataTable[] { outputTableMatched.getTable(), outputTableMissed.getTable() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];
		return new DataTableSpec[] { outSpec, outSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		ElementFilterSettings tmpSettings = new ElementFilterSettings();
		tmpSettings.loadSettings(settings);
		if ((tmpSettings.targetColumn() == null) || (tmpSettings.targetColumn().length() == 0)) {
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
}
