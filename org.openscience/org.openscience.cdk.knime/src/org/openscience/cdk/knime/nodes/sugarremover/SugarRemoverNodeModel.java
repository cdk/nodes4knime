/*
 * Copyright (c) 2013, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.nodes.sugarremover;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell3;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * This is the model implementation of SugarRemover.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SugarRemoverNodeModel extends CDKAdapterNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SugarRemoverNodeModel.class);

	/**
	 * Constructor for the node model.
	 */
	protected SugarRemoverNodeModel() {
		super(1, 1, new SugarRemoverSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		String[] smilesList = { "C(C(C(C(C(C=O)O)O)O)O)O", "C(C(CC(C(CO)O)O)O)(O)=O", "C(C(C(CC(=O)O)O)O)O",
				"C(C(C(C(C(CO)O)O)O)=O)O", "C(C(C(C(C(CO)O)O)O)O)O", "C(C(C(C(CC=O)O)O)O)O", "occ(o)co",
				"OCC(O)C(O)C(O)C(O)CO", "O=CC(O)C(O)C(O)C(O)CO", "CC(=O)OCC(O)CO", "CCCCC(O)C(=O)O",
				"CC(=O)CC(=O)CCC(=O)O", "CC(O)C(O)C(=O)O", "O=C(O)CC(O)CC(=O)O", "O=C(O)C(=O)C(=O)C(O)C(O)CO",
				"CC(O)CC(=O)O", "CC(CCC(=O)O)CC(=O)O", "O=C(O)CCC(O)C(=O)O", "O=CC(O)C(O)C(O)C(O)CO",
				"O=C(CO)C(O)C(O)CO" };
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		List<IAtomContainer> sugarChains = new ArrayList<IAtomContainer>();

		try {
			for (String smiles : smilesList) {
				sugarChains.add(sp.parseSmiles(smiles));
			}
		} catch (InvalidSmilesException ex) {
			LOGGER.error(ex.getMessage());
		}

		BufferedDataContainer outputTable = exec.createDataContainer(appendSpec(convertedTables[0].getDataTableSpec()));

		SugarRemoverWorker worker = new SugarRemoverWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec,
				outputTable, settings(SugarRemoverSettings.class), sugarChains);
		
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
			outputTable.close();
		}

		return new BufferedDataTable[] { outputTable.getTable() };
	}
	
	private DataTableSpec appendSpec(DataTableSpec spec) {
		
		DataColumnSpec cs;
		if (settings(SugarRemoverSettings.class).replaceColumn()) {
			DataColumnSpec[] dcs = new DataColumnSpec[spec.getNumColumns()];
			int i = 0;
			for (DataColumnSpec s : spec) {
				if (i == columnIndex) {
					String name = spec.getColumnNames()[columnIndex];
					dcs[i] = new DataColumnSpecCreator(name, CDKCell3.TYPE).createSpec();
				} else {
					dcs[i] = s;
				}
				i++;
			}
			return new DataTableSpec(dcs);
		} else {
			String name = DataTableSpec.getUniqueColumnName(spec, settings(SugarRemoverSettings.class)
					.appendColumnName());
			cs = new DataColumnSpecCreator(name, CDKCell3.TYPE).createSpec();
			return new DataTableSpec(spec, new DataTableSpec(cs));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];

		// check if there is any issue with the name of the column to be appended
		if (!settings(SugarRemoverSettings.class).replaceColumn()) {
			String name = settings(SugarRemoverSettings.class).appendColumnName();

			if (name == null || name.length() == 0) {
				throw new InvalidSettingsException("Invalid name for appended column");
			} else if (inSpecs[0].containsName(name)) {
				throw new InvalidSettingsException("Duplicate column name: " + name);
			}
		}

		return new DataTableSpec[] { appendSpec(outSpec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SugarRemoverSettings s = new SugarRemoverSettings();
		s.loadSettings(settings);

		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No compatible molecule column chosen");
		}
	}
}
