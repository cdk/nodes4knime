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
package org.openscience.cdk.knime.sketcher;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKAdapterCell;
import org.openscience.cdk.knime.type.CDKCell3;

/**
 * @author wiswedel, University of Konstanz
 */
public class SketcherNodeModel extends NodeModel {

	static final String CFG_STRUCTURE = "structure";

	private String smiles;

	public SketcherNodeModel() {
		super(0, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		return new DataTableSpec[] { generateSpec() };
	}

	private DataTableSpec generateSpec() {

		DataColumnSpec s = new DataColumnSpecCreator("Structure", CDKAdapterCell.RAW_TYPE).createSpec();
		return new DataTableSpec("Structure Table", s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		BufferedDataContainer c = exec.createDataContainer(generateSpec());

		IAtomContainer mol = CDKNodeUtils.getFullMolecule(smiles);
		mol = CDKNodeUtils.calculateCoordinates(mol, false);
		DataCell cell = CDKCell3.createCDKCell(mol);
		c.addRowToTable(new DefaultRow(new RowKey("Structure"), cell));

		c.close();
		return new BufferedDataTable[] { c.getTable() };
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
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (smiles != null) {
			settings.addString(CFG_STRUCTURE, smiles);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		smiles = settings.getString(CFG_STRUCTURE, (String) null);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String smiles = settings.getString(CFG_STRUCTURE, "");
		if (smiles == null || smiles.length() == 0) {
			// ignore
		}
		try {
			IAtomContainer mol = CDKNodeUtils.getFullMolecule(smiles);
			mol = CDKNodeUtils.calculateCoordinates(mol, false);
			@SuppressWarnings("unused")
			DataCell c = CDKCell3.createCDKCell(mol);
		} catch (Exception exception) {
			throw new InvalidSettingsException("Can't parse SMILES string: " + smiles);
		}
	}
}
