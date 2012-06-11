/*
 * Created on 31.01.2007 18:31:38 by thor ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- *
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
