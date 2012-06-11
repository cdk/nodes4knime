/*
 * ------------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 * 
 * History 16.09.2008 (thor): created
 */
package org.openscience.cdk.knime.convert.cdk2molecule;

import java.io.File;
import java.io.IOException;

import org.knime.chem.types.CMLCell;
import org.knime.chem.types.Mol2Cell;
import org.knime.chem.types.SdfCell;
import org.knime.chem.types.SmilesCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.convert.cdk2molecule.CDK2MoleculeSettings.Format;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This class is the model for the CDK->Molecule node. It converts CDK molecules into different textual representations.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDK2MoleculeNodeModel extends NodeModel {

	private final CDK2MoleculeSettings m_settings = new CDK2MoleculeSettings();

	/**
	 * Creates a new model.
	 */
	public CDK2MoleculeNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_settings.columnName() == null) {
			for (DataColumnSpec cs : inSpecs[0]) {
				if (cs.getType().isCompatible(CDKValue.class)) {
					if (m_settings.columnName() != null) {
						setWarningMessage("Selected column '" + m_settings.columnName() + "' as CDK column");
					} else {
						m_settings.columnName(cs.getName());
					}
				}
			}
			if (m_settings.columnName() == null) {
				throw new InvalidSettingsException("No CDK column in input table");
			}
		} else {
			if (!inSpecs[0].containsName(m_settings.columnName())) {
				throw new InvalidSettingsException("Column '" + m_settings.columnName()
						+ "' does not exist in input table");
			}
			if (!inSpecs[0].getColumnSpec(m_settings.columnName()).getType().isCompatible(CDKValue.class)) {
				throw new InvalidSettingsException("Column '" + m_settings.columnName()
						+ "' does not contain CDK molecules");
			}
		}

		return new DataTableSpec[] { createRearranger(inSpecs[0]).createSpec() };
	}

	private ColumnRearranger createRearranger(final DataTableSpec inSpec) {

		ColumnRearranger crea = new ColumnRearranger(inSpec);

		DataType type = null;
		if (m_settings.destFormat() == Format.SDF) {
			type = SdfCell.TYPE;
		} else if (m_settings.destFormat() == Format.Smiles) {
			type = SmilesCell.TYPE;
		} else if (m_settings.destFormat() == Format.Mol2) {
			type = Mol2Cell.TYPE;
		} else if (m_settings.destFormat() == Format.CML) {
			type = CMLCell.TYPE;
		}

		DataColumnSpec cs;
		if (m_settings.replaceColumn()) {
			cs = new DataColumnSpecCreator(m_settings.columnName(), type).createSpec();
		} else {
			String name = DataTableSpec.getUniqueColumnName(inSpec, m_settings.newColumnName());
			cs = new DataColumnSpecCreator(name, type).createSpec();
		}

		MolConverter conv = new MolConverter(cs, inSpec.findColumnIndex(m_settings.columnName()));

		if (m_settings.replaceColumn()) {
			crea.replace(conv, m_settings.columnName());
		} else {
			crea.append(conv);
		}

		return crea;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger crea = createRearranger(inData[0].getDataTableSpec());

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], crea, exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

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

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

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

		CDK2MoleculeSettings s = new CDK2MoleculeSettings();
		s.loadSettings(settings);

		if (s.columnName() == null) {
			throw new InvalidSettingsException("No column selected");
		}

		if (!s.replaceColumn() && s.newColumnName().length() < 1) {
			throw new InvalidSettingsException("No name for the new column entered");
		}
	}
}
