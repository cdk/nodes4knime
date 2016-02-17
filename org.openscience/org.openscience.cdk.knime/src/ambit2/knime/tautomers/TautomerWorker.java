/*
 * Copyright (c) 2014, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package ambit2.knime.tautomers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

import ambit2.tautomers.TautomerManager;

/**
 * Threaded worker for AMBIT's tautomer generator.
 * 
 * @author Stephan Beisken, EMBL-EBI
 */
public class TautomerWorker extends MultiThreadWorker<DataRow, DataRow[]> {

	// execution mode
	private Mode mode;
	// total number of input rows
	private final double maxRows;
	// target row column index
	private final int columnIndex;
	// execution context of the node
	private final ExecutionContext exec;
	// output data container to be written to
	private final BufferedDataContainer bdc;

	/*
	 * Execution modes of the worker:
	 * 
	 * ALL - generate all tautomers and append (plus score)
	 * BEST_APPEND - generate only the best tautomer and append (plus score)
	 * BEST_APPEND - generate only the best tautomer and replace (no score)
	 */
	protected enum Mode {
		ALL, BEST_APPEND, BEST_REPLACE
	};

	/**
	 * Constructor for AMBIT's tautomer generator worker.
	 * 
	 * @param maxQueueSize maximum queue size of finished jobs
	 * @param maxActiveInstanceSize maximum number of simultaneously running computations
	 * @param maxRows total number of input rows
	 * @param exec execution context of the node
	 * @param bdc output data container to be written to
	 * @param columnIndex target row column index
	 */
	public TautomerWorker(final int maxQueueSize, final int maxActiveInstanceSize, final long maxRows,
			final ExecutionContext exec, final BufferedDataContainer bdc, final int columnIndex) {

		super(maxQueueSize, maxActiveInstanceSize);

		mode = Mode.BEST_REPLACE;

		this.bdc = bdc;
		this.exec = exec;
		this.maxRows = maxRows;
		this.columnIndex = columnIndex;
	}

	/**
	 * Sets the execution mode: ALL, BEST_APPEND, BEST_REPLACE
	 * 
	 * @param mode the execution mode
	 */
	protected void executionMode(final Mode mode) {
		this.mode = mode;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataRow[] compute(DataRow row, long index) throws Exception {

		// check whether the target cell in the data row is either missing or corrupted
		if (missingOrBroken(row)) {
			// return missing data cells where necessary
			return missingRows(row);
		}

		// get the CDK cell from the data row: adapter cells are used
		CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
		// get the CDK atom container from the CDK cell
		IAtomContainer molecule = cdkCell.getAtomContainer();

		// instantiate the tautomer generator with the current molecule
		TautomerManager tautomerManager = new TautomerManager();

		// tautomer manager configuration (static for now)
		// n.b. no idea what all those filters do exactly
		tautomerManager.tautomerFilter.FlagApplyWarningFilter = true;
		tautomerManager.tautomerFilter.FlagApplyExcludeFilter = true;
		tautomerManager.tautomerFilter.FlagApplyDuplicationFilter = true;
		tautomerManager.tautomerFilter.FlagApplyDuplicationCheckIsomorphism = true;
		tautomerManager.tautomerFilter.FlagApplyDuplicationCheckInChI = false;
		tautomerManager.tautomerFilter.FlagFilterIncorrectValencySumStructures = true;
		tautomerManager.tautomerFilter.FlagApplySimpleAromaticityRankCorrection = true;

		tautomerManager.FlagCheckDuplicationOnRegistering = true;

		tautomerManager.FlagRecurseBackResultTautomers = false;

		tautomerManager.FlagPrintTargetMoleculeInfo = false;
		tautomerManager.FlagPrintExtendedRuleInstances = true;
		tautomerManager.FlagPrintIcrementalStepDebugInfo = false;

		tautomerManager.getKnowledgeBase().activateChlorineRules(false);
		tautomerManager.getKnowledgeBase().activateRingChainRules(false);
		// tautomerManager.getKnowledgeBase().use13ShiftRulesOnly(true);
		tautomerManager.getKnowledgeBase().use15ShiftRules(true);
		tautomerManager.getKnowledgeBase().use17ShiftRules(false);

		tautomerManager.maxNumOfBackTracks = 10000;

		tautomerManager.setStructure(molecule);

		// generate all tautomers
		Vector<IAtomContainer> resultTautomers = tautomerManager.generateTautomersIncrementaly();

		if (mode == Mode.ALL) { // append all tautomers including their score
			if (resultTautomers != null) { // fall through if null

				// define output row array
				DataRow[] outRows = new DataRow[resultTautomers.size()];
				int rowCounter = 0;

				// sort the tautomer vector in increasing order of the tautomers' score
				Collections.sort(resultTautomers, new ScoreComparator());
				for (IAtomContainer tautomer : resultTautomers) {
					Object rank_property = tautomer.getProperty("TAUTOMER_RANK");
					if (rank_property != null) { // fall through if null
						double rank = Double.parseDouble(rank_property.toString());
						// standardise molecule for CDK cells
						tautomer = CDKNodeUtils.getFullMolecule(tautomer);

						// create output cells and row
						DataCell rankCell = new DoubleCell(rank);
						DataCell tautomerCell = CDKCell.createCDKCell(tautomer);
						DataCell[] outCells = new DataCell[] { tautomerCell, rankCell };
						RowKey key = new RowKey(row.getKey() + "_" + rowCounter);
						DataRow outRow = new AppendedColumnRow(key, row, outCells);
						outRows[rowCounter++] = outRow;
					}
				}
				DataRow[] outRowsFinal = new DataRow[rowCounter];
				System.arraycopy(outRows, 0, outRowsFinal, 0, rowCounter);

				// return final row array for post-processing
				return outRowsFinal;
			}
		} else {
			double bestRank = Double.MAX_VALUE;
			IAtomContainer bestTautomer = null;

			if (resultTautomers != null) { // fall through if null
				for (IAtomContainer tautomer : resultTautomers) {
					Object rankProperty = tautomer.getProperty("TAUTOMER_RANK");
					if (rankProperty != null) { // no rank, fall through
						double rank = Double.parseDouble(rankProperty.toString());

						// rank is energy based, lower rank is better
						if ((bestTautomer == null) || (bestRank > rank)) {
							bestRank = rank;
							bestTautomer = tautomer;
						}
					}
				}

				if (bestTautomer != null) {
					// standardise molecule for CDK cells
					bestTautomer = CDKNodeUtils.getFullMolecule(bestTautomer);

					// create output cells and row
					DataCell tautomerCell = CDKCell.createCDKCell(bestTautomer);
					DataRow outRow = null;
					if (mode == Mode.BEST_REPLACE) { // replace the CDK cell
						outRow = new ReplacedColumnsDataRow(row, tautomerCell, columnIndex);
					} else if (mode == Mode.BEST_APPEND) { // append the CDK cell and the score
						DataCell rankCell = new DoubleCell(bestRank);
						DataCell[] outCells = new DataCell[] { tautomerCell, rankCell };
						outRow = new AppendedColumnRow(row, outCells);
					}

					DataRow[] outRowsFinal = new DataRow[] { outRow };

					// return final row array for post-processing
					return outRowsFinal;
				}
			}
		}

		// return missing data cells where necessary
		// previous actions did not yield viable tautomers
		return missingRows(row);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processFinished(ComputationTask task)
			throws ExecutionException, CancellationException, InterruptedException {

		for (DataRow row : task.get()) {
			bdc.addRowToTable(row);
		}

		exec.setProgress(this.getFinishedCount() / maxRows, progressMessage());

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}

	/**
	 * Returns whether the target cell in the data row is either missing or its adapter cell is corrupted.
	 * 
	 * @param row the input data row
	 * @return if missing or corrupted
	 */
	private boolean missingOrBroken(DataRow row) {
		return (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null));
	}

	/**
	 * Returns the missing row output based on the worker's mode.
	 * 
	 * @param row the input row
	 * @return the output missing row array
	 */
	private DataRow[] missingRows(DataRow row) {

		DataRow[] outRows;
		if (mode == Mode.BEST_REPLACE) { // return the original row and CDK cell
			outRows = new DataRow[] { row };
		} else { // return the original row and missing cells
			DataCell[] outCells = new DataCell[2];
			Arrays.fill(outCells, DataType.getMissingCell());
			outRows = new DataRow[] { new AppendedColumnRow(row, outCells) };
		}
		return outRows;
	}

	/**
	 * Returns the progress message to be displayed on the node. Total finished jobs, active, and queued jobs are shown.
	 * 
	 * @return the progress message
	 */
	private String progressMessage() {
		return this.getFinishedCount() + " (active/submitted: " + this.getActiveCount() + "/"
				+ (this.getSubmittedCount() - this.getFinishedCount()) + ")";
	}

	/**
	 * Comparator based on an atom container's tautomer rank as assigned by the tautomer generator.
	 * 
	 * Tautomers are sorted in increasing order.
	 * 
	 * @author Stephan Beisken
	 */
	class ScoreComparator implements Comparator<IAtomContainer> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compare(IAtomContainer o1, IAtomContainer o2) {

			Object rankPropertyO1 = o1.getProperty("TAUTOMER_RANK");
			Object rankPropertyO2 = o2.getProperty("TAUTOMER_RANK");

			if (rankPropertyO1 == null) {
				rankPropertyO1 = Double.MAX_VALUE;
			}
			if (rankPropertyO2 == null) {
				rankPropertyO2 = Double.MAX_VALUE;
			}

			double rankO1 = Double.parseDouble(rankPropertyO1.toString());
			double rankO2 = Double.parseDouble(rankPropertyO2.toString());

			return rankO1 < rankO2 ? -1 : (rankO1 == rankO2 ? 0 : 1);
		}

	}
}
