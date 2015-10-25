/*
 * Copyright (C) 2003 - 2016 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.nodes.sssearch;

import java.util.concurrent.ExecutionException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKCell3;
import org.openscience.cdk.knime.util.JMolSketcherPanel;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * This is the model for the substructure search node. It divides the input
 * table into two output tables. One with all molecules that contain a certain
 * substucture and the the other with the molecules that don't.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class SSSearchNodeModel extends CDKAdapterNodeModel {

	private IAtomContainer m_fragment;

	/**
	 * Creates a new model with 1 input and 2 output ports.
	 */
	public SSSearchNodeModel() {
		super(1, 2, new SSSearchSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] process(BufferedDataTable[] convertedTables, ExecutionContext exec) throws Exception {

		BufferedDataContainer outputTableMatched = exec.createDataContainer(appendSpec(convertedTables[0]
				.getDataTableSpec()));
		BufferedDataContainer outputTableMissed = exec.createDataContainer(appendSpec(convertedTables[0]
				.getDataTableSpec()));

		SSSearchWorker worker = new SSSearchWorker(maxQueueSize, maxParallelWorkers, columnIndex,
				convertedTables[0].getRowCount(), exec, m_fragment, outputTableMatched, outputTableMissed);
		worker.highlight(settings(SSSearchSettings.class).isHighlight());
		worker.charge(settings(SSSearchSettings.class).isCharge());
		worker.exactMatch(settings(SSSearchSettings.class).isExactMatch());

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

		try {
			String smiles = settings(SSSearchSettings.class).getSmiles();
			if (smiles != null && !smiles.isEmpty()) {
				m_fragment = JMolSketcherPanel.readStringNotation(settings(SSSearchSettings.class).getSmiles())
						.getAtomContainer(0);
				if (m_fragment.getAtomCount() != 0) {
					CDKNodeUtils.getFullMolecule(m_fragment);
				}
			} else {
				m_fragment = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
			}
		} catch (Exception ex) {
			throw new InvalidSettingsException("Unable to read fragment", ex);
		}

		autoConfigure(inSpecs);
		DataTableSpec outSpec = convertTables(inSpecs)[0];
		return new DataTableSpec[] { appendSpec(outSpec), appendSpec(outSpec) };
	}

	private DataTableSpec appendSpec(DataTableSpec spec) {

		DataTableSpec outSpec = spec;
		if (settings(SSSearchSettings.class).isHighlight()) {
			DataColumnSpec[] outCSpec = new DataColumnSpec[outSpec.getNumColumns()];
			int i = 0;
			for (DataColumnSpec cSpec : spec) {
				if (i == columnIndex) {
					String name = outSpec.getColumnNames()[columnIndex];
					outCSpec[i] = new DataColumnSpecCreator(name, CDKCell3.TYPE).createSpec();
				} else {
					outCSpec[i] = cSpec;
				}
				i++;
			}
			outSpec = new DataTableSpec(outCSpec);
		}
		return outSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		m_fragment = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SSSearchSettings s = new SSSearchSettings();
		s.loadSettings(settings);
		try {
			if (s.getSmiles() != null && !s.getSmiles().isEmpty()) {
				JMolSketcherPanel.readStringNotation(s.getSmiles());
			}
		} catch (Exception ex) {
			throw new InvalidSettingsException("Unable to read fragment", ex);
		}
	}
}
