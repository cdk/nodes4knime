/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.fingerprints.similarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.knime.nodes.fingerprints.similarity.SimilaritySettings.AggregationMethod;
import org.openscience.cdk.knime.nodes.fingerprints.similarity.SimilaritySettings.ReturnType;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * This is the model implementation of the similarity node. CDK is used to
 * calculate the Tanimoto coefficient for two fingerprints. The minimum, maximum
 * or average can be selected as aggregation method.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilarityNodeModel extends CDKNodeModel {

	private Map<BitSet, ArrayList<String>> fingerprintRefs;
	private List<BitSet> matrixFingerprintRefs;
	private int rowCount;

	/**
	 * Constructor for the node model.
	 */
	protected SimilarityNodeModel() {
		super(2, 1, new SimilaritySettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		String sr = settings.targetColumn();
		final int fingerprintRefColIndex = inData[1].getDataTableSpec().findColumnIndex(sr);
		fingerprintRefs = getFingerprintRefs(inData[1], fingerprintRefColIndex);
		matrixFingerprintRefs = getMatrixRefs(inData[1], fingerprintRefColIndex);
		rowCount = fingerprintRefs.size();

		ColumnRearranger cr = createColumnRearranger(inData[0].getDataTableSpec());
		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], cr, exec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		final int fingerprintColIndex = spec.findColumnIndex(settings.targetColumn());

		DataColumnSpec[] outSpec = createSpec(spec);

		AbstractCellFactory cf = new AbstractCellFactory(true, outSpec) {

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
					if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Matrix) {
						List<DataCell> results = new ArrayList<DataCell>();
						for (BitSet refBs : matrixFingerprintRefs) {
							if (refBs == null)
								results.add(DataType.getMissingCell());
							else
								results.add(new DoubleCell(Tanimoto.calculate(bs, refBs)));
						}
						cells[0] = CollectionCellFactory.createListCell(results);
					} else {
						float coeff = 0.0f;
						float pcoeff = 0.0f;
						ArrayList<String> pkey = null;
						Iterator<Map.Entry<BitSet, ArrayList<String>>> it = fingerprintRefs.entrySet().iterator();

						if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Minimum) {
							pcoeff = 1;
							while (it.hasNext()) {
								Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
								coeff = Tanimoto.calculate(bs, pairs.getKey());
								if (coeff <= pcoeff) {
									pcoeff = coeff;
									pkey = pairs.getValue();
								}
							}

						} else if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Maximum) {

							if (settings(SimilaritySettings.class).identical()) {
								while (it.hasNext()) {
									Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
									coeff = Tanimoto.calculate(bs, pairs.getKey());
									if (coeff >= pcoeff) {
										if (pairs.getValue().contains(row.getKey().getString())) {
											if (pairs.getValue().size() > 1) {
												ArrayList<String> keys = pairs.getValue();
												keys.remove(row.getKey().getString());
												pcoeff = coeff;
												pkey = keys;
											}
										} else {
											pcoeff = coeff;
											pkey = (ArrayList<String>) pairs.getValue();
										}
									}
								}
							} else {
								while (it.hasNext()) {
									Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
									coeff = Tanimoto.calculate(bs, pairs.getKey());
									if (coeff >= pcoeff) {
										pcoeff = coeff;
										pkey = (ArrayList<String>) pairs.getValue();
									}
								}
							}

						} else if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Average) {
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
							if (settings(SimilaritySettings.class).returnType().equals(ReturnType.String)) {
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
							} else if (settings(SimilaritySettings.class).returnType().equals(ReturnType.Collection)) {
								cells[1] = CollectionCellFactory.createListCell(res);
							}
						}
					}
				} catch (CDKException exception) {
					Arrays.fill(cells, DataType.getMissingCell());
				}
				return cells;
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.append(cf);
		return arranger;
	}

	private DataColumnSpec[] createSpec(final DataTableSpec oldSpec) {

		DataColumnSpec[] outSpec = null;
		String uniqueColName = DataTableSpec.getUniqueColumnName(oldSpec, "Tanimoto");
		String uniqueColRefName = DataTableSpec.getUniqueColumnName(oldSpec, "Reference");
		if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Average) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(uniqueColName, DoubleCell.TYPE).createSpec();
			outSpec = new DataColumnSpec[] { colSpec };
			
		} else if (settings(SimilaritySettings.class).aggregationMethod() == AggregationMethod.Matrix) {
			DataColumnSpec colSpec = new DataColumnSpecCreator(uniqueColName, ListCell.getCollectionType(DoubleCell.TYPE))
					.createSpec();
			outSpec = new DataColumnSpec[] { colSpec };
		} else {
			DataColumnSpec colSpec1 = new DataColumnSpecCreator(uniqueColName, DoubleCell.TYPE).createSpec();
			DataColumnSpec colSpec2 = null;
			if (settings(SimilaritySettings.class).returnType().equals(ReturnType.String)) {
				colSpec2 = new DataColumnSpecCreator(uniqueColRefName, StringCell.TYPE).createSpec();
			} else if (settings(SimilaritySettings.class).returnType().equals(ReturnType.Collection)) {
				colSpec2 = new DataColumnSpecCreator(uniqueColRefName, ListCell.getCollectionType(StringCell.TYPE))
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
	 * Provides a list of bitsets in their given order.
	 * 
	 * @param bdt a buffered data table with DenseBitVector cells
	 * @param fingerprintRefColIndex a fingerprint column index in bdt
	 * @return the map
	 */
	private List<BitSet> getMatrixRefs(DataTable dt, int fingerprintRefColIndex) {

		List<BitSet> fingerprintRefs = new ArrayList<BitSet>();

		for (DataRow row : dt) {
			if (row.getCell(fingerprintRefColIndex).isMissing()) {
				fingerprintRefs.add(null);
				continue;
			}
			BitVectorValue bitVectorValue = (BitVectorValue) row.getCell(fingerprintRefColIndex);
			String bitString = bitVectorValue.toBinaryString();
			BitSet bs = new BitSet((int) bitVectorValue.length());

			for (int j = 0; j < bitString.length(); j++) {
				if (bitString.charAt(j) == '1')
					bs.set(j);
			}
			fingerprintRefs.add(bs);
		}

		return fingerprintRefs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		settings.targetColumn(CDKNodeUtils.autoConfigure(inSpecs[0], settings.targetColumn(), BitVectorValue.class));

		if (settings.targetColumn() == null || (inSpecs[1].findColumnIndex(settings.targetColumn())) == -1) {
			String name = null;
			for (DataColumnSpec s : inSpecs[1]) {
				if (s.getType().isCompatible(BitVectorValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				settings.targetColumn(name);
			} else {
				throw new InvalidSettingsException("No reference DenseBitVector compatible column in input table");
			}
		}

		// creates the column rearranger -- does the heavy lifting for adapter
		// cells
		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		SimilaritySettings s = new SimilaritySettings();
		s.loadSettings(settings);
	}
}
