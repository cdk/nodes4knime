/*
 * Copyright (c) 2016, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.nodes.atomsignature;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.nodes.atomsignature.AtomSignatureSettings.SignatureTypes;

/**
 * This is the model implementation of AtomSignature.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class AtomSignatureNodeModel extends CDKAdapterNodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected AtomSignatureNodeModel() {
		super(1, 1, new AtomSignatureSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		DataTableSpec convertedSpec = convertedTables[0].getDataTableSpec();
		DataTableSpec appendSpec = appendSpec(convertedSpec);
		DataTableSpec outSpec = new DataTableSpec(convertedSpec, appendSpec);

		BufferedDataContainer outputTable = exec.createDataContainer(outSpec);

		AtomSignatureWorker worker = new AtomSignatureWorker(maxQueueSize, maxParallelWorkers, columnIndex, exec,
				outputTable, settings(AtomSignatureSettings.class));

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];
		DataTableSpec appendSpec = appendSpec(outSpec);

		return new DataTableSpec[] { new DataTableSpec(outSpec, appendSpec) };
	}

	private DataTableSpec appendSpec(final DataTableSpec inSpecs) {

		int addNbColumns = settings(AtomSignatureSettings.class).isHeightSet() ? settings(AtomSignatureSettings.class)
				.getMaxHeight() - settings(AtomSignatureSettings.class).getMinHeight() + 2 : 2;

		DataColumnSpec[] cs = new DataColumnSpec[addNbColumns];
		for (int i = 0; i < addNbColumns; i++) {

			String name = null;
			if (i == 0) {
				name = DataTableSpec.getUniqueColumnName(inSpecs, "Atom ID");
			} else {
				if (settings(AtomSignatureSettings.class).signatureType().equals(SignatureTypes.AtomSignatures)) {
					name = DataTableSpec.getUniqueColumnName(inSpecs,
							"Signature " + (i - 1 + settings(AtomSignatureSettings.class).getMinHeight()));
				} else {
					name = DataTableSpec.getUniqueColumnName(inSpecs,
							"HOSE " + (i - 1 + settings(AtomSignatureSettings.class).getMinHeight()));
				}
				name = DataTableSpec.getUniqueColumnName(inSpecs, name);
			}
			cs[i] = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
		}

		return new DataTableSpec(cs);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		AtomSignatureSettings s = new AtomSignatureSettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
		if (settings(AtomSignatureSettings.class).isHeightSet()) {
			Integer minHeight = settings(AtomSignatureSettings.class).getMinHeight();
			Integer maxHeight = settings(AtomSignatureSettings.class).getMaxHeight();
			if (minHeight == null || maxHeight == null || minHeight > maxHeight) {
				throw new InvalidSettingsException("Heights wrongly defined");
			}
		}
	}
}
