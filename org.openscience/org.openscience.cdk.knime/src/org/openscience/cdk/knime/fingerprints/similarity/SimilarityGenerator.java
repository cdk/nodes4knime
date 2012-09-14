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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.ExecutionMonitor;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.AggregationMethod;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.ReturnType;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * Cell factory utilizing CDK's Tanimoto functionality to calculate fingerprint distances.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilarityGenerator implements CellFactory {

	private final int rowCount;
	private final int targetColIndex;
	private final DataColumnSpec[] dataColumnSpec;
	private final ReturnType returnType;
	private final AggregationMethod aggregationMethod;
	private final Map<BitSet, ArrayList<String>> referenceMap;

	/**
	 * Constructs a cell factory for the fingerprint similarity node.
	 * 
	 * @param dataColumnSpec output column spec to be appended
	 * @param targetColIndex column index of DenseBitVector cell in input table
	 * @param referenceMap bitset - row key associations
	 * @param aggregationMethod min, max, or avg aggregation method
	 * @param rowCount number of rows in the input table (for avg aggregation)
	 */
	public SimilarityGenerator(DataColumnSpec[] dataColumnSpec, int targetColIndex,
			Map<BitSet, ArrayList<String>> referenceMap, ReturnType returnType, AggregationMethod aggregationMethod,
			int rowCount) {

		this.dataColumnSpec = dataColumnSpec;
		this.referenceMap = referenceMap;
		this.targetColIndex = targetColIndex;
		this.returnType = returnType;
		this.aggregationMethod = aggregationMethod;

		this.rowCount = rowCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(DataRow row) {

		DataCell dataCell = row.getCell(targetColIndex);
		DataCell[] newCells = new DataCell[dataColumnSpec.length];
		if (dataCell.isMissing()) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}
		if (!(dataCell instanceof DenseBitVectorCell)) {
			throw new IllegalArgumentException("No String cell at " + targetColIndex + ": "
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
			Iterator<Map.Entry<BitSet, ArrayList<String>>> it = referenceMap.entrySet().iterator();
			if (aggregationMethod.equals(AggregationMethod.Minimum)) {
				pcoeff = 1;
				while (it.hasNext()) {
					Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
					coeff = Tanimoto.calculate(bs, pairs.getKey());
					if (coeff <= pcoeff) {
						pcoeff = coeff;
						pkey = pairs.getValue();
					}
				}
			} else if (aggregationMethod.equals(AggregationMethod.Maximum)) {
				while (it.hasNext()) {
					Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
					coeff = Tanimoto.calculate(bs, pairs.getKey());
					if (coeff >= pcoeff) {
						pcoeff = coeff;
						pkey = (ArrayList<String>) pairs.getValue();
					}
				}
			} else if (aggregationMethod.equals(AggregationMethod.Average)) {
				while (it.hasNext()) {
					Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
					coeff += Tanimoto.calculate(bs, pairs.getKey());
				}
				pcoeff = coeff / rowCount;
				pkey = new ArrayList<String>();
			}

			newCells[0] = new DoubleCell(pcoeff);
			List<StringCell> res = new ArrayList<StringCell>();
			for (String st : pkey) {
				res.add(new StringCell(st));
			}

			if (res.size() > 0) {
				if (returnType.equals(ReturnType.String)) {
					if (res.size() == 1)
						newCells[1] = res.get(0);
					else {
						String resString = "";
						for (StringCell cell : res) {
							resString += (cell.getStringValue() + "|");
						}
						resString = resString.substring(0, resString.lastIndexOf("|"));
						newCells[1] = new StringCell(resString);
					}
				} else if (returnType.equals(ReturnType.Collection)) {
					newCells[1] = CollectionCellFactory.createListCell(res);
					;
				}
			}
		} catch (CDKException exception) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}
		return newCells;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return dataColumnSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {

		exec.setProgress(curRowNr / (double) rowCount, "Retrieved conversions for row " + curRowNr + " (\"" + lastKey
				+ "\")");
	}
}
