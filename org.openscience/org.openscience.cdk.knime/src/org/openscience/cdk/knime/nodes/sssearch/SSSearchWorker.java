/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.interfaces.ITetrahedralChirality;
import org.openscience.cdk.interfaces.ITetrahedralChirality.Stereo;
import org.openscience.cdk.isomorphism.AtomMatcher;
import org.openscience.cdk.isomorphism.BondMatcher;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.isomorphism.VentoFoggia;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainerCreator;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell3;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.PseudoAtom;

import com.google.common.base.Predicate;

/**
 * Multi threaded worker implementation for the Substructure Search Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SSSearchWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final double max;
	private final Set<Long> matchedRows;
	private final BufferedDataContainer[] bdcs;

	private final Pattern pattern;
	private final IAtomContainer query;

	private boolean highlight;
	private boolean charge;
	private boolean exactMatch;

	private static final int MAX_MATCHES = 5;

	public SSSearchWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final long max, final ExecutionContext exec, final IAtomContainer fragment,
			final BufferedDataContainer... bdcs) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.max = max;
		this.columnIndex = columnIndex;
		this.bdcs = bdcs;
		this.query = fragment;
		
		boolean hasPseudoAtoms = false;
		for (IAtom atom : fragment.atoms()) {
			if (atom instanceof PseudoAtom) {
				hasPseudoAtoms = true;
				break;
			}
		}
		if (hasPseudoAtoms) {
			IAtomContainer queryFragment = QueryAtomContainerCreator.createAnyAtomForPseudoAtomQueryContainer(fragment);
			this.pattern = VentoFoggia.findSubstructure(queryFragment, AtomMatcher.forQuery(), BondMatcher.forQuery());
		} else {
			this.pattern = VentoFoggia.findSubstructure(fragment);
		}
		
		highlight = false;
		charge = false;
		exactMatch = false;
		matchedRows = Collections.synchronizedSet(new HashSet<Long>());
	}

	public void highlight(boolean highlight) {
		this.highlight = highlight;
	}

	public void charge(boolean charge) {
		this.charge = charge;
	}
	
	public void exactMatch(boolean exactMatch) {
		this.exactMatch = exactMatch;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			return row;
		}

		CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer mol = cdkCell.getAtomContainer();

		if (pattern.matches(mol)) {

			List<Predicate<int[]>> predicates = new ArrayList<Predicate<int[]>>();
			if (charge) {
				predicates.add(new ChargePredicate(query, mol));
			}
			if (exactMatch) {
				predicates.add(new ExactStereoPredicate(query, mol));
			}

			Mappings mappings = pattern.matchAll(mol).stereochemistry().uniqueAtoms();
			for (Predicate<int[]> predicate : predicates) {
				mappings = mappings.filter(predicate);
			}
			mappings = mappings.limit(MAX_MATCHES);

			if (mappings.atLeast(1)) {

				matchedRows.add(index);

				if (highlight) {

					int i = 0;
					Color[] color = CDKNodeUtils.generateColorPalette();

					for (Map<IAtom, IAtom> map : mappings.toAtomMap()) {
						for (Map.Entry<IAtom, IAtom> e : map.entrySet()) {
							e.getValue().setProperty(StandardGenerator.HIGHLIGHT_COLOR, color[i]);
						}
						i++;
					}

					int j = 0;
					for (Map<IBond, IBond> map : mappings.toBondMap()) {
						for (Map.Entry<IBond, IBond> e : map.entrySet()) {
							e.getValue().setProperty(StandardGenerator.HIGHLIGHT_COLOR, color[j]);
						}
						j++;
					}

					row = new ReplacedColumnsDataRow(row, CDKCell3.createCDKCell(mol), columnIndex);
				}
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

		exec.setProgress(
				this.getFinishedCount() / max,
				this.getFinishedCount() + " (active/submitted: " + this.getActiveCount() + "/"
						+ (this.getSubmittedCount() - this.getFinishedCount()) + ")");

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}

	class ChargePredicate implements Predicate<int[]> {

		private final IAtomContainer query;
		private final IAtomContainer target;

		public ChargePredicate(IAtomContainer query, IAtomContainer target) {

			this.query = query;
			this.target = target;
		}

		@Override
		public boolean apply(int[] m) {
			// mapping: query to target
			for (int n = 0; n < m.length; n++) {
				if (query.getAtom(n).getFormalCharge() != target.getAtom(m[n]).getFormalCharge()) {
					return false;
				}
			}
			return true;
		}
	}

	class ExactStereoPredicate implements Predicate<int[]> {

		private final Stereo[] query;
		private final Stereo[] target;

		public ExactStereoPredicate(IAtomContainer query, IAtomContainer target) {

			this.query = init(query);
			this.target = init(target);
		}
		
		private Stereo[] init(IAtomContainer molecule) {
			
			Stereo[] stereo = new Stereo[molecule.getAtomCount()];
			for (IStereoElement stereoElement : molecule.stereoElements()) {
				if (stereoElement instanceof ITetrahedralChirality) {
					ITetrahedralChirality chirality = (ITetrahedralChirality) stereoElement;
					stereo[molecule.getAtomNumber(chirality.getChiralAtom())] = chirality.getStereo();
				}
			}
			return stereo;
		}
		
		@Override
		public boolean apply(int[] m) {
			// mapping: query to target
			for (int n = 0; n < m.length; n++) {
				if (query[n] == null && target[m[n]] != null) {
					return false;
				}
			}
			return true;
		}
	}
}
