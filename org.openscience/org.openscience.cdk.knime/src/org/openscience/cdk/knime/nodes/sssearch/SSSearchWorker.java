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
import java.util.Map;
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
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
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

	private final Pattern pattern;

	private boolean highlight;
	private boolean charge;

	private static final int MAX_MATCHES = 5;

	public SSSearchWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final IAtomContainer fragment, final BufferedDataContainer... bdcs) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.columnIndex = columnIndex;
		this.bdcs = bdcs;
		this.pattern = VentoFoggia.findSubstructure(fragment);

		highlight = false;
		charge = false;
		matchedRows = new HashSet<Long>();
	}

	public void highlight(boolean highlight) {
		this.highlight = highlight;
	}
	
	public void charge(boolean charge) {
		this.charge = charge;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			return row;
		}

		CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer mol = cdkCell.getAtomContainer();

		if (pattern.match(mol).length > 0) {

			Set<Integer> excluded = null;
			
			if (charge) {

				int i = 0;
				int j = 0;
				excluded = new HashSet<Integer>();
				Mappings mappings = pattern.matchAll(mol).limit(MAX_MATCHES).stereochemistry().uniqueAtoms();
				for (Map<IAtom, IAtom> map : mappings.toAtomMap()) {
					for (Map.Entry<IAtom, IAtom> e : map.entrySet()) {
						if (e.getKey().getFormalCharge() != e.getValue().getFormalCharge()) {
							excluded.add(j);
							i++;
							break;
						}
					}
					j++;
				}
				if (i == j) {
					return row;
				}
			}
			
			matchedRows.add(index);

			if (highlight) {

				int i = 0;
				Color[] color = CDKNodeUtils.generateColors(MAX_MATCHES);

				Mappings mappings = pattern.matchAll(mol).limit(MAX_MATCHES).stereochemistry().uniqueAtoms();
				for (Map<IAtom, IAtom> map : mappings.toAtomMap()) {
					if (excluded != null && excluded.contains(i)) {
						continue;
					}
					for (Map.Entry<IAtom, IAtom> e : map.entrySet()) {
						e.getValue().setProperty(CDKConstants.ANNOTATIONS, color[i].getRGB());
					}
					i++;
				}

				int j = 0;
				for (Map<IBond, IBond> map : mappings.toBondMap()) {
					if (excluded != null && excluded.contains(i)) {
						continue;
					}
					for (Map.Entry<IBond, IBond> e : map.entrySet()) {
						e.getValue().setProperty(CDKConstants.ANNOTATIONS, color[j].getRGB());
					}
					j++;
				}

				row = new ReplacedColumnsDataRow(row, CDKCell.createCDKCell(mol), columnIndex);
			}
			// } else if (highlight) {
			// row = new ReplacedColumnsDataRow(row, CDKCell.createCDKCell(mol),
			// columnIndex);
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
