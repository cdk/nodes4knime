/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
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
package org.openscience.cdk.knime.connectivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.base.node.parallel.builder.ThreadedTableBuilderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.RowAppender;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model for the connectivity node that performs all computation by using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeModel extends ThreadedTableBuilderNodeModel {

	private final ConnectivitySettings m_settings = new ConnectivitySettings();
	private int molColIndex;

	/**
	 * Creates a new model with one input and one output port.
	 */
	public ConnectivityNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());

		if (m_settings.addFragmentColumn()) {
			String name = DataTableSpec.getUniqueColumnName(data[0].getDataTableSpec(), "Fragments");
			DataColumnSpec cs = new DataColumnSpecCreator(name, SetCell.getCollectionType(CDKCell.TYPE)).createSpec();
			return new DataTableSpec[] { new DataTableSpec(data[0].getDataTableSpec(), new DataTableSpec(cs)) };
		} else {
			return new DataTableSpec[] { data[0].getDataTableSpec() };
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (molCol >= 0) {
						molCol = -1;
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
					}
				}
			}

			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				m_settings.molColumnName(name);
			}
		}

		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColumnName() + "' does not exist");
		}

		if (m_settings.addFragmentColumn()) {
			String name = DataTableSpec.getUniqueColumnName(inSpecs[0], "Fragments");
			DataColumnSpec cs = new DataColumnSpecCreator(name, SetCell.getCollectionType(CDKCell.TYPE)).createSpec();
			return new DataTableSpec[] { AppendedColumnTable.getTableSpec(inSpecs[0], cs) };
		} else {
			return inSpecs;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		if (inRow.getCell(molColIndex).isMissing()) {
			if (m_settings.addFragmentColumn()) {
				outputTables[0].addRowToTable(new AppendedColumnRow(inRow, CollectionCellFactory
						.createSetCell(Collections.singleton(DataType.getMissingCell()))));
			} else {
				outputTables[0].addRowToTable(inRow);
			}
			return;
		}
		if (m_settings.removeCompleteRow()) {
			removeRows(inRow, outputTables);
		} else if (m_settings.addFragmentColumn()) {
			addFragments(inRow, outputTables);
		} else {
			retainBiggest(inRow, outputTables);
		}
	}

	private void removeRows(final DataRow inRow, final RowAppender[] outputTables) {

		IAtomContainer mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();
		if (ConnectivityChecker.isConnected(mol)) {
			outputTables[0].addRowToTable(inRow);
		}
	}

	private void retainBiggest(final DataRow inRow, final RowAppender[] outputTables) throws CanceledExecutionException {

		IAtomContainer mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();

		if (!ConnectivityChecker.isConnected(mol)) {
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			IAtomContainer biggest = molSet.getAtomContainer(0);
			for (int i = 1; i < molSet.getAtomContainerCount(); i++) {
				if (molSet.getAtomContainer(i).getBondCount() > biggest.getBondCount()) {
					biggest = molSet.getAtomContainer(i);
				}
			}
			outputTables[0].addRowToTable(new ReplacedColumnsDataRow(inRow, new CDKCell(biggest), molColIndex));
		} else {
			outputTables[0].addRowToTable(inRow);
		}
	}

	private void addFragments(final DataRow inRow, final RowAppender[] outputTables) throws CanceledExecutionException {

		IAtomContainer mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();

		if (!ConnectivityChecker.isConnected(mol)) {
			IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
			List<DataCell> cells = new ArrayList<DataCell>(molSet.getAtomContainerCount());

			IAtomContainer singleMol;
			for (int i = 0; i < molSet.getAtomContainerCount(); i++) {
				singleMol = molSet.getAtomContainer(i);
				cells.add(new CDKCell(singleMol));
			}

			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, CollectionCellFactory.createSetCell(cells)));
		} else {
			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, CollectionCellFactory.createSetCell(Collections
					.singleton(new CDKCell(mol)))));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
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
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		m_settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		ConnectivitySettings s = new ConnectivitySettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
	}
}
