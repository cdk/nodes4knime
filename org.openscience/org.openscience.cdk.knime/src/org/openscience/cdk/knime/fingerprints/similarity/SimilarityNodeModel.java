/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
import org.knime.core.data.collection.ListCell;
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
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.ReturnType;

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
			DataColumnSpec colSpec2 = null;
			if (m_settings.returnType().equals(ReturnType.String)) {
				colSpec2 = new DataColumnSpecCreator("Reference", StringCell.TYPE).createSpec();
			} else if (m_settings.returnType().equals(ReturnType.Collection)) {
				colSpec2 = new DataColumnSpecCreator("Reference", ListCell.getCollectionType(StringCell.TYPE))
						.createSpec();
			}
			outSpec = new DataColumnSpec[] { colSpec1, colSpec2 };
		}
		SimilarityGenerator generator = new SimilarityGenerator(outSpec, fingerprintColIndex, fingerprintRefs,
				m_settings.returnType(), m_settings.aggregationMethod(), rowCount);
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
