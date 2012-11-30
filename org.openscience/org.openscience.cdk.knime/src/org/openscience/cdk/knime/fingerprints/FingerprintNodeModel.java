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
package org.openscience.cdk.knime.fingerprints;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.fingerprints.FingerprintSettings.FingerprintTypes;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model for the fingerprint node. It uses the CDK to create fingerprints (which are essentially bit sets)
 * for the molecules in the input table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * 
 */
public class FingerprintNodeModel extends ThreadedColAppenderNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(FingerprintNodeModel.class);

	private final FingerprintSettings m_settings = new FingerprintSettings();

	/**
	 * Creates a new model for the fingerprint node.
	 */
	public FingerprintNodeModel() {

		super(1, 1);

		this.setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final IFingerprinter fp;
		if (m_settings.fingerprintType().equals(FingerprintTypes.Extended)) {
			fp = new ExtendedFingerprinter();
		} else if (m_settings.fingerprintType().equals(FingerprintTypes.EState)) {
			fp = new EStateFingerprinter();
		} else if (m_settings.fingerprintType().equals(FingerprintTypes.Pubchem)) {
			fp = new PubchemFingerprinter();
		} else if (m_settings.fingerprintType().equals(FingerprintTypes.MACCS)) {
			fp = new MACCSFingerprinter();
		} else {
			fp = new Fingerprinter();
		}

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumn());

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell[] cells = new DataCell[1];

				if (row.getCell(colIndex).isMissing()) {
					cells[0] = DataType.getMissingCell();
					return cells;
				}

				DataCell oldCell = row.getCell(colIndex);
				final IAtomContainer mol = ((CDKValue) oldCell).getAtomContainer();

				try {
					BitSet fingerprint;
					IAtomContainer con = CDKNodeUtils.getExplicitClone(mol);
					fingerprint = fp.getBitFingerprint(con).asBitSet();
					// transfer the bitset into a dense bit vector
					DenseBitVector bitVector = new DenseBitVector(fingerprint.size());
					for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
						bitVector.set(i);
					}
					DenseBitVectorCellFactory fact = new DenseBitVectorCellFactory(bitVector);
					return new DataCell[] { fact.createDataCell() };
				} catch (Exception ex) {
					LOGGER.error("Error while creating fingerprints", ex);
					return new DataCell[] { DataType.getMissingCell() };
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				DataColumnSpecCreator crea = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(
						data[0].getDataTableSpec(), m_settings.molColumn()), DenseBitVectorCell.TYPE);
				return new DataColumnSpec[] { crea.createSpec() };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (m_settings.molColumn() == null) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isCompatible(CDKValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.molColumn(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else {
				throw new InvalidSettingsException("No CDK compatible column in input table");
			}
		}

		DataColumnSpec colspec = inSpecs[0].getColumnSpec(m_settings.molColumn());
		if (!colspec.getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("No CDK column: " + m_settings.molColumn());
		}
		String newColName = m_settings.fingerprintType() + " fingerprints for " + m_settings.molColumn();
		newColName = DataTableSpec.getUniqueColumnName(inSpecs[0], newColName);

		DataColumnSpecCreator crea = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpecs[0],
				m_settings.molColumn()), DenseBitVectorCell.TYPE);
		DataTableSpec outSpec = AppendedColumnTable.getTableSpec(inSpecs[0], crea.createSpec());

		return new DataTableSpec[] { outSpec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do;
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

		m_settings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		FingerprintSettings s = new FingerprintSettings();
		s.loadSettings(settings);
	}
}
