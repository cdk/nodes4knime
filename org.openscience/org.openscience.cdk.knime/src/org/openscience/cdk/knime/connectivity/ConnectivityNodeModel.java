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

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model for the connectivity node that performs all computation by using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ConnectivityNodeModel extends NodeModel {

	private final ConnectivitySettings m_settings = new ConnectivitySettings();

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
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		final int molColIndex = inData[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());

		BufferedDataTable res;
		if (m_settings.removeCompleteRow()) {
			res = removeRows(inData[0], exec, molColIndex);
		} else if (m_settings.addFragmentColumn()) {
			res = addFragments(inData[0], exec, molColIndex);
		} else {
			res = retainBiggest(inData[0], exec, molColIndex);
		}

		return new BufferedDataTable[] { res };
	}

	private BufferedDataTable removeRows(final BufferedDataTable in, final ExecutionContext exec, final int molColIndex) {

		final double max = in.getRowCount();

		BufferedDataContainer cont = exec.createDataContainer(in.getDataTableSpec());
		int count = 0;
		int removed = 0;
		exec.setMessage("");
		for (DataRow row : in) {
			exec.setProgress(count++ / max);
			if (row.getCell(molColIndex).isMissing()) {
				cont.addRowToTable(row);
				continue;
			}

			IAtomContainer mol = null;
			try {
				mol = (IAtomContainer) ((CDKValue) row.getCell(molColIndex)).getAtomContainer().clone();
			} catch (CloneNotSupportedException exception) {
				setWarningMessage("Clone not supported.");
			}

			if (ConnectivityChecker.isConnected(mol)) {
				cont.addRowToTable(row);
			} else {
				exec.setMessage("Removed " + ++removed + " molecules");
			}
		}
		cont.close();

		return cont.getTable();
	}

	private BufferedDataTable retainBiggest(final BufferedDataTable in, final ExecutionContext exec,
			final int molColIndex) throws CanceledExecutionException {

		ColumnRearranger crea = new ColumnRearranger(in.getDataTableSpec());
		SingleCellFactory cf = new SingleCellFactory(in.getDataTableSpec().getColumnSpec(molColIndex)) {

			@Override
			public DataCell getCell(final DataRow row) {

				if (row.getCell(molColIndex).isMissing()) {
					return DataType.getMissingCell();
				}

				IAtomContainer mol = null;
				try {
					mol = (IAtomContainer) ((CDKValue) row.getCell(molColIndex)).getAtomContainer().clone();
				} catch (CloneNotSupportedException exception) {
					setWarningMessage("Clone not supported.");
				}

				if (!ConnectivityChecker.isConnected(mol)) {
					IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
					IAtomContainer biggest = molSet.getAtomContainer(0);
					for (int i = 1; i < molSet.getAtomContainerCount(); i++) {
						if (molSet.getAtomContainer(i).getBondCount() > biggest.getBondCount()) {
							biggest = molSet.getAtomContainer(i);
						}
					}

					return new CDKCell(biggest);
				} else {
					return row.getCell(molColIndex);
				}
			}
		};
		crea.replace(cf, molColIndex);

		return exec.createColumnRearrangeTable(in, crea, exec);
	}

	private BufferedDataTable addFragments(final BufferedDataTable in, final ExecutionContext exec,
			final int molColIndex) throws CanceledExecutionException {

		ColumnRearranger crea = new ColumnRearranger(in.getDataTableSpec());

		String name = DataTableSpec.getUniqueColumnName(in.getDataTableSpec(), "Fragments");
		DataColumnSpec cs = new DataColumnSpecCreator(name, SetCell.getCollectionType(CDKCell.TYPE)).createSpec();

		SingleCellFactory cf = new SingleCellFactory(cs) {

			@Override
			public DataCell getCell(final DataRow row) {

				if (row.getCell(molColIndex).isMissing()) {
					return DataType.getMissingCell();
				}

				IAtomContainer mol = ((CDKValue) row.getCell(molColIndex)).getAtomContainer();

				if (!ConnectivityChecker.isConnected(mol)) {
					IAtomContainerSet molSet = ConnectivityChecker.partitionIntoMolecules(mol);
					List<DataCell> cells = new ArrayList<DataCell>(molSet.getAtomContainerCount());

					IAtomContainer singleMol;
					for (int i = 0; i < molSet.getAtomContainerCount(); i++) {
						singleMol = molSet.getAtomContainer(i);
						// remove JCP valency labels
						for (IAtom atom : singleMol.atoms()) {
							atom.setValency(null);
						}
						cells.add(new CDKCell(singleMol));
					}
					// remove JCP valency labels
					for (IAtom atom : mol.atoms()) {
						atom.setValency(null);
					}

					return CollectionCellFactory.createSetCell(cells);
				} else {
					return CollectionCellFactory.createSetCell(Collections.singleton(new CDKCell(mol)));
				}
			}
		};
		crea.append(cf);

		return exec.createColumnRearrangeTable(in, crea, exec);
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
