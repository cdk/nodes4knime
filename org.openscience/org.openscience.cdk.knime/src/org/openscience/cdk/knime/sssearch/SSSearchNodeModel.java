/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.sssearch;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * This is the model for the substructure search node. It divides the input table into two output tables. One with all
 * molecules that contain a certain substucture and the the other with the molecules that don't.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class SSSearchNodeModel extends ThreadedTableBuilderNodeModel {

	private IAtomContainer m_fragment;

	private int m_columnIndex;

	private final SSSearchSettings m_settings = new SSSearchSettings();

	/**
	 * Creates a new model with 1 input and 2 output ports.
	 */
	public SSSearchNodeModel() {

		super(1, 2);
		
		// add search term flow variable
//		pushFlowVariableString("smiles", value)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		m_columnIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColName());

		return new DataTableSpec[] { data[0].getDataTableSpec(), data[0].getDataTableSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		if (inRow.getCell(m_columnIndex).isMissing()) {
			outputTables[1].addRowToTable(inRow);
		} else {
			IAtomContainer mol = ((CDKValue) inRow.getCell(m_columnIndex)).getAtomContainer();
			boolean hasAromaticFlag = false;
			for (int i = 0; i < mol.getAtomCount(); i++) {
				if (mol.getAtom(i).getFlag(CDKConstants.ISAROMATIC)) {
					hasAromaticFlag = true;
					break;
				}
			}

			if (!hasAromaticFlag) {
				CDKHueckelAromaticityDetector.detectAromaticity(mol);
			}

			if (UniversalIsomorphismTester.isSubgraph(mol, m_fragment)) {
				outputTables[0].addRowToTable(inRow);
			} else {
				outputTables[1].addRowToTable(inRow);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_settings.molColName() == null) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (m_settings.molColName() != null) {
						setWarningMessage("Using '" + m_settings.molColName() + "' as molecule column");
						break;
					} else {
						m_settings.molColName(dcs.getName());
					}
				}
			}

			if (m_settings.molColName() == null) {
				throw new InvalidSettingsException("No CDK column in input table");
			}
		}

		DataColumnSpec s = inSpecs[0].getColumnSpec(m_settings.molColName());
		if (s == null || !s.getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColName()
					+ "' does not exist or is incompatible");
		}

		try {
			m_fragment = createMolecule(m_settings.smilesFragments());
		} catch (CDKException ex) {
			throw new InvalidSettingsException("Unable to read fragment", ex);
		}
		return new DataTableSpec[] { inSpecs[0], inSpecs[0] };
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_settings.loadSettings(settings);
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
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SSSearchSettings s = new SSSearchSettings();
		s.loadSettings(settings);
		try {
			createMolecule(s.smilesFragments());
		} catch (CDKException ex) {
			throw new InvalidSettingsException("Unable to read fragment", ex);
		}
	}

	/**
	 * Creates a molecule from the given Smiles string.
	 * 
	 * @param smiles molecules as Smiles strings
	 * @return a molecule
	 * @throws CDKException if parsing the Smiles string fails
	 */
	private static IAtomContainer createMolecule(final String... smiles) throws CDKException {

		if ((smiles != null) && (smiles.length > 0)) {
			SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
			IAtomContainer mol = parser.parseSmiles(smiles[0]);
			CDKHueckelAromaticityDetector.detectAromaticity(mol);
			return mol;
		} else {
			return SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
		}
	}
}
