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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.node.ExecutionMonitor;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.AggregationMethod;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * Cell factory utilizing CDK's Tanimoto functionality to calculate fingerprint distances.
 * 
 * @author Stephan Beisken
 */
public class SimilarityGenerator implements CellFactory {

	private final int rowCount;
	private final int targetColIndex;
	private final DataColumnSpec[] dataColumnSpec;
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
			Map<BitSet, ArrayList<String>> referenceMap, AggregationMethod aggregationMethod, int rowCount) {

		this.dataColumnSpec = dataColumnSpec;
		this.referenceMap = referenceMap;
		this.targetColIndex = targetColIndex;
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
			String res = "";
			for (String st : pkey) {
				if (res.equals("")) {
					res += st;
				} else {
					res += "|" + st;
				}
			}

			if (res.length() > 0) {
				newCells[1] = new StringCell(res);
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
