/*
 * Copyright (c) 2012, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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

package org.openscience.cdk.knime.symmetrycalculator;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.RowAppender;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.invariant.EquivalentClassPartitioner;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;

/**
 * This is the model implementation of SymmetryCalculator.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class SymmetryCalculatorNodeModel extends ThreadedTableBuilderNodeModel {

	static final String VISUAL = "visual";
	static final String CFG_COLNAME = "colName";

	private String colName;
	private boolean visual;
	private int molColIndex;
	private int addColumns;

	/**
	 * Constructor for the node model.
	 */
	protected SymmetryCalculatorNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		molColIndex = data[0].getDataTableSpec().findColumnIndex(colName);
		addColumns = (!visual) ? 2 : 1;
		;

		DataColumnSpec[] clmspecs = configCSpecs(data[0].getDataTableSpec());

		return new DataTableSpec[] { new DataTableSpec(clmspecs) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		IAtomContainer mol = null;
		if (!inRow.getCell(molColIndex).isMissing()) {
			mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();
		}

		// check if cell is missing and return a missing value in that case
		if (mol == null || !ConnectivityChecker.isConnected(mol)) {
			DataCell[] missings = new DataCell[addColumns];
			Arrays.fill(missings, DataType.getMissingCell());
			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, missings));
			return;
		} else {
			mol = SMSDNormalizer.convertExplicitToImplicitHydrogens(mol);

			int atomId = 1;
			Map<String, Integer> parentIdMap = new HashMap<String, Integer>();
			// make a map of the sequential numbering based
			if (CDKNodePlugin.numbering() == NUMBERING.SEQUENTIAL) {
				for (IAtom atom : mol.atoms()) {
					parentIdMap.put(atom.getID(), atomId);
					atomId++;
				}
			}

			// partition the atoms into equivalent classes
			int[] partitions = null;
			try {
				partitions = new EquivalentClassPartitioner(mol).getTopoEquivClassbyHuXu(mol);
			} catch (Exception exception) {
				// do nothing
			}
			int count = 0;

			if (partitions == null || partitions[partitions.length - 1] > partitions[0]) {
				DataCell[] missings = new DataCell[addColumns];
				Arrays.fill(missings, DataType.getMissingCell());
				outputTables[0].addRowToTable(new AppendedColumnRow(inRow, missings));
				return;
			}

			int[] counts = new int[partitions[0]];
			int parts = 0;
			for (int i = 1; i < partitions.length; i++) {
				counts[partitions[i] - 1]++;
				if (counts[partitions[i] - 1] == 2) parts++;
			}

			if (!visual) {
				for (int i = 1; i < partitions.length; i++) {

					DataCell[] outCells = new DataCell[addColumns];
					if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
						outCells[0] = new StringCell(mol.getAtom(i - 1).getID());
					} else {
						outCells[0] = new StringCell("" + parentIdMap.get(mol.getAtom(i - 1).getID()));
					}
					outCells[1] = new IntCell(partitions[i]);
					outputTables[0].addRowToTable(new AppendedColumnRow(new RowKey(inRow.getKey().getString() + "_"
							+ count), inRow, outCells));

					count++;
				}
			} else {
				int k = 0;
				Color[] colors = CDKNodeUtils.generateColors(parts);
				Map<Integer, Integer> colorsUsed = new HashMap<Integer, Integer>();
				
				for (int i = 1; i < partitions.length; i++) {
					if (counts[partitions[i] - 1] > 1) {
						if (!colorsUsed.containsKey(partitions[i] - 1)) {
							colorsUsed.put(partitions[i] - 1, k);
							k++;
						}
						mol.getAtom(i - 1).setProperty(CDKConstants.ANNOTATIONS, colors[colorsUsed.get(partitions[i] - 1)].getRGB());
					}
				}
				DataCell[] image = new DataCell[] { new CDKCell(mol) };
				outputTables[0].addRowToTable(new AppendedColumnRow(inRow, image));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		String autoColName = "";
		int molCol = inSpecs[0].findColumnIndex(colName);
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {

				if (dcs.getType().isCompatible(CDKValue.class)) {

					if (molCol >= 0) {
						molCol = -1;
						autoColName = "";
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
						autoColName = dcs.getName();
					}
				}
			}
			// if there is no molcolumn at this point complaint
			if (molCol == -1) {
				throw new InvalidSettingsException("Molecule column '" + colName + "' does not exist");
			}
			colName = autoColName;
		}

		DataColumnSpec[] newCs = configCSpecs(inSpecs[0]);

		return new DataTableSpec[] { new DataTableSpec(newCs) };
	}

	private DataColumnSpec[] configCSpecs(final DataTableSpec inSpecs) {

		// add a new column to the specs...
		int inNbColumns = inSpecs.getNumColumns();
		int addColumns = (!visual) ? 2 : 1;

		DataColumnSpec[] cs = new DataColumnSpec[inNbColumns + addColumns];

		// copy the columnspecs from the previous table
		for (int i = 0; i < inNbColumns; i++)
			cs[i] = inSpecs.getColumnSpec(i);

		if (!visual) {
			// add the columnspecs for the new columns
			for (int i = inNbColumns; i < (inNbColumns + addColumns); i++) {
				String name = null;
				if (i == inNbColumns) {
					name = "Atom ID";
					cs[i] = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
				} else {
					// Check if the name we want for the new column already
					// exists and if so generate a different one
					name = DataTableSpec.getUniqueColumnName(inSpecs, "Equivalent Class");
					cs[i] = new DataColumnSpecCreator(name, IntCell.TYPE).createSpec();
				}

			}
		} else {
			// Check if the name we want for the new column already exists and
			// if so generate a different one
			String name = DataTableSpec.getUniqueColumnName(inSpecs, "Equivalent Class Rendering");
			cs[inNbColumns] = new DataColumnSpecCreator(name, CDKCell.TYPE).createSpec();
		}

		return cs;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		if (colName != null) {
			settings.addString(CFG_COLNAME, colName);
		}
		settings.addBoolean(VISUAL, visual);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		colName = settings.getString(CFG_COLNAME);
		visual = settings.getBoolean(VISUAL, false);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		String colName = settings.getString(CFG_COLNAME);
		if ((colName == null) || (colName.length() < 1)) {
			throw new InvalidSettingsException("No column choosen");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

}
