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
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.AggregationMethod;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.ReturnType;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * This is the model implementation of the similarity node. CDK is used to calculate the Tanimoto coefficient for two
 * fingerprints. The minimum, maximum or average can be selected as aggregation method.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilarityNodeModel extends ThreadedColAppenderNodeModel {

	private int rowCount;
	private final SimilaritySettings m_settings = new SimilaritySettings();

	/**
	 * Constructor for the node model.
	 */
	protected SimilarityNodeModel() {

		super(2, 1);
		
		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		String sr = m_settings.fingerprintRefColumn();
		final int fingerprintRefColIndex = data[1].getDataTableSpec().findColumnIndex(sr);
		final Map<BitSet, ArrayList<String>> fingerprintRefs = getFingerprintRefs(data[1], fingerprintRefColIndex);
		rowCount = fingerprintRefs.size();

		final int fingerprintColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.fingerprintColumn());

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell dataCell = row.getCell(fingerprintColIndex);
				DataCell[] cells = new DataCell[getColumnSpecs().length];
				if (dataCell.isMissing()) {
					Arrays.fill(cells, DataType.getMissingCell());
					return cells;
				}
				if (!(dataCell instanceof DenseBitVectorCell)) {
					throw new IllegalArgumentException("No String cell at " + fingerprintColIndex + ": "
							+ dataCell.getClass().getName());
				}
				DenseBitVectorCell bitVectorCell = (DenseBitVectorCell) dataCell;

				String bitString = bitVectorCell.toBinaryString();
				BitSet bs = new BitSet((int) bitVectorCell.length());
				for (int j = 0; j < bitString.length(); j++) {
					if (bitString.charAt(j) == '1')
						bs.set(j);
				}

				try {
					float coeff = 0.0f;
					float pcoeff = 0.0f;
					ArrayList<String> pkey = null;
					Iterator<Map.Entry<BitSet, ArrayList<String>>> it = fingerprintRefs.entrySet().iterator();
					if (m_settings.aggregationMethod().equals(AggregationMethod.Minimum)) {
						pcoeff = 1;
						while (it.hasNext()) {
							Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
							coeff = Tanimoto.calculate(bs, pairs.getKey());
							if (coeff <= pcoeff) {
								pcoeff = coeff;
								pkey = pairs.getValue();
							}
						}
					} else if (m_settings.aggregationMethod().equals(AggregationMethod.Maximum)) {
						while (it.hasNext()) {
							Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
							coeff = Tanimoto.calculate(bs, pairs.getKey());
							if (coeff >= pcoeff) {
								pcoeff = coeff;
								pkey = (ArrayList<String>) pairs.getValue();
							}
						}
					} else if (m_settings.aggregationMethod().equals(AggregationMethod.Average)) {
						while (it.hasNext()) {
							Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
							coeff += Tanimoto.calculate(bs, pairs.getKey());
						}
						pcoeff = coeff / rowCount;
						pkey = new ArrayList<String>();
					}

					cells[0] = new DoubleCell(pcoeff);
					List<StringCell> res = new ArrayList<StringCell>();
					for (String st : pkey) {
						res.add(new StringCell(st));
					}

					if (res.size() > 0) {
						if (m_settings.returnType().equals(ReturnType.String)) {
							if (res.size() == 1)
								cells[1] = res.get(0);
							else {
								String resString = "";
								for (StringCell cell : res) {
									resString += (cell.getStringValue() + "|");
								}
								resString = resString.substring(0, resString.lastIndexOf("|"));
								cells[1] = new StringCell(resString);
							}
						} else if (m_settings.returnType().equals(ReturnType.Collection)) {
							cells[1] = CollectionCellFactory.createListCell(res);
						}
					}
				} catch (CDKException exception) {
					Arrays.fill(cells, DataType.getMissingCell());
					return cells;
				}
				return cells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return createSpec(data[0].getDataTableSpec());
			}
		};

		return new ExtendedCellFactory[] { cf };
	}

	private DataColumnSpec[] createSpec(final DataTableSpec oldSpec) {

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

		return outSpec;
	}

	/**
	 * Provides a map of bitsets and their corresponding rows.
	 * 
	 * @param bdt a buffered data table with DenseBitVector cells
	 * @param fingerprintRefColIndex a fingerprint column index in bdt
	 * @return the map
	 */
	private Map<BitSet, ArrayList<String>> getFingerprintRefs(DataTable dt, int fingerprintRefColIndex) {

		Map<BitSet, ArrayList<String>> fingerprintRefs = new HashMap<BitSet, ArrayList<String>>();

		for (DataRow row : dt) {
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

		DataTableSpec outSpec = new DataTableSpec(createSpec(inSpecs[0]));
		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], outSpec) };
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
