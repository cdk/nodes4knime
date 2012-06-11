/*
 * Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
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
package org.openscience.cdk.knime.fingerprints.similarity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of the similarity node. CDK is used to calculate the Tanimoto coefficient for two
 * fingerprints. The minimum, maximum or average can be selected as aggregation method.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilarityNodeModel extends NodeModel {

	private int rowCount;
	private Map<BitSet, ArrayList<String>> fingerprintRefs;
	private final SimilaritySettings m_settings = new SimilaritySettings();

	/**
	 * Constructor for the node model.
	 */
	protected SimilarityNodeModel() {

		super(2, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		DataTableSpec spec = inData[0].getDataTableSpec();
		DataTableSpec specRef = inData[1].getDataTableSpec();

		String sr = m_settings.fingerprintRefColumn();
		final int fingerprintRefColIndex = specRef.findColumnIndex(sr);

		fingerprintRefs = getFingerprintRefs(inData[1], fingerprintRefColIndex);
		rowCount = inData[1].getRowCount();

		ColumnRearranger rearranger = createColumnRearranger(spec);
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0], rearranger, exec);

		return new BufferedDataTable[] { outTable };
	}

	/**
	 * Provides a map of bitsets and their corresponding rows.
	 * 
	 * @param bdt a buffered data table with DenseBitVector cells
	 * @param fingerprintRefColIndex a fingerprint column index in bdt
	 * @return the map
	 */
	private Map<BitSet, ArrayList<String>> getFingerprintRefs(BufferedDataTable bdt, int fingerprintRefColIndex) {

		Map<BitSet, ArrayList<String>> fingerprintRefs = new HashMap<BitSet, ArrayList<String>>();

		for (DataRow row : bdt) {
			if (row.getCell(fingerprintRefColIndex).isMissing()) {
				continue;
			}
			BitVectorValue bitVectorValue = (BitVectorValue) row.getCell(fingerprintRefColIndex);
			String bitString = bitVectorValue.toBinaryString();
			BitSet bs = new BitSet((int) bitVectorValue.length());

			for (int j = 0; j < bitString.length(); j++) {
				if (bitString.charAt(j) == '1')
					bs.set(j);
			}
			if (fingerprintRefs.containsKey(bs)) {
				fingerprintRefs.get(bs).add(row.getKey().getString());
			} else {
				ArrayList<String> keyList = new ArrayList<String>();
				keyList.add(row.getKey().getString());
				fingerprintRefs.put(bs, keyList);
			}
		}
		return fingerprintRefs;
	}

	/**
	 * Creates a column rearranger to append one/two output columns.
	 * 
	 * @param spec a input table specification
	 * @return the rearranger
	 * @throws InvalidSettingsException unexpected behaviour
	 */
	private ColumnRearranger createColumnRearranger(DataTableSpec spec) throws InvalidSettingsException {

		int fingerprintColIndex = spec.findColumnIndex(m_settings.fingerprintColumn());
		DataColumnSpec[] outSpec = null;
		if (m_settings.aggregationMethod().name().equals("Average")) {
			DataColumnSpec colSpec = new DataColumnSpecCreator("Tanimoto", DoubleCell.TYPE).createSpec();
			outSpec = new DataColumnSpec[] { colSpec };
		} else {
			DataColumnSpec colSpec1 = new DataColumnSpecCreator("Tanimoto", DoubleCell.TYPE).createSpec();
			DataColumnSpec colSpec2 = new DataColumnSpecCreator("Reference", StringCell.TYPE).createSpec();
			outSpec = new DataColumnSpec[] { colSpec1, colSpec2 };
		}
		SimilarityGenerator generator = new SimilarityGenerator(outSpec, fingerprintColIndex, fingerprintRefs,
				m_settings.aggregationMethod(), rowCount);
		ColumnRearranger arrange = new ColumnRearranger(spec);
		arrange.append(generator);

		return arrange;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_settings.fingerprintColumn() == null
				|| (inSpecs[0].findColumnIndex(m_settings.fingerprintColumn())) == -1) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isCompatible(BitVectorValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.fingerprintColumn(name);
			} else {
				throw new InvalidSettingsException("No DenseBitVector compatible column in input table");
			}
		}
		if (m_settings.fingerprintRefColumn() == null
				|| (inSpecs[1].findColumnIndex(m_settings.fingerprintRefColumn())) == -1) {
			String name = null;
			for (DataColumnSpec s : inSpecs[1]) {
				if (s.getType().isCompatible(BitVectorValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.fingerprintRefColumn(name);
			} else {
				throw new InvalidSettingsException("No reference DenseBitVector compatible column in input table");
			}
		}

		DataTableSpec outSpec = createColumnRearranger(inSpecs[0]).createSpec();
		return new DataTableSpec[] { outSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettingsTo(settings);
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
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SimilaritySettings s = new SimilaritySettings();
		s.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do;
	}
}
