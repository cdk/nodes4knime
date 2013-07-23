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
package org.openscience.cdk.knime.nodes.sssearch;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Multi threaded worker implementation for the Substructure Search Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SSSearchWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final Set<Long> matchedRows;
	private final BufferedDataContainer[] bdcs;
	private final IAtomContainer fragment;
	private final UniversalIsomorphismTester isomorphismTester;
	
	private boolean highlight;

	public SSSearchWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final IAtomContainer fragment, final BufferedDataContainer... bdcs) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.columnIndex = columnIndex;
		this.bdcs = bdcs;
		this.fragment = fragment;

		highlight = false;
		matchedRows = new HashSet<Long>();
		isomorphismTester = new UniversalIsomorphismTester();
	}
	
	public void highlight(boolean highlight) {
		this.highlight = highlight;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			// fall through
		} else {
			CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
			IAtomContainer mol = cdkCell.getAtomContainer();

			if (isomorphismTester.isSubgraph(mol, fragment)) {

				if (highlight) {
					List<List<RMap>> atomMaps = isomorphismTester.getSubgraphAtomsMaps(mol, fragment);
					Color[] color = CDKNodeUtils.generateColors(atomMaps.size());
					int i = 0;
					for (List<RMap> atomMap : atomMaps) {
						for (RMap map : atomMap) {
							mol.getAtom(map.getId1()).setProperty(CDKConstants.ANNOTATIONS, color[i].getRGB());
						}
						i++;
					}

					List<List<RMap>> bondMaps = isomorphismTester.getSubgraphMaps(mol, fragment);
					i = 0;
					for (List<RMap> bondMap : bondMaps) {
						for (RMap map : bondMap) {
							mol.getBond(map.getId1()).setProperty(CDKConstants.ANNOTATIONS, color[i].getRGB());
						}
						i++;
					}

					row = new ReplacedColumnsDataRow(row, CDKCell.createCDKCell(mol), columnIndex);
					matchedRows.add(index);
				} else {
					matchedRows.add(index);
				}
			} else if (highlight) {
				row = new ReplacedColumnsDataRow(row, CDKCell.createCDKCell(mol), columnIndex);
			}
		}

		return row;
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		long index = task.getIndex();
		DataRow replace = task.get();
		if (matchedRows.contains(index)) {
			bdcs[0].addRowToTable(replace);
		} else {
			bdcs[1].addRowToTable(replace);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
