/*
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
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
package org.openscience.cdk.knime.fingerprints;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCell;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
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
 */
public class FingerprintNodeModel extends NodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(FingerprintNodeModel.class);

	private final FingerprintSettings m_settings = new FingerprintSettings();

	/**
	 * Creates a new model for the fingerprint node.
	 */
	public FingerprintNodeModel() {

		super(1, 1);
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
		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
	}

	private ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		String s = m_settings.molColumn();
		if (s == null || !spec.containsName(s)) {
			throw new InvalidSettingsException("No such column: " + s);
		}
		DataColumnSpec colspec = spec.getColumnSpec(s);
		if (!colspec.getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("No CDK column: " + s);
		}

		String newColName = m_settings.fingerprintType() + " fingerprints for " + s;
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);

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

		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DenseBitVectorCell.TYPE);
		DataColumnSpec appendSpec = c.createSpec();
		final int molColIndex = spec.findColumnIndex(s);
		SingleCellFactory cf = new SingleCellFactory(appendSpec) {

			@Override
			public DataCell getCell(final DataRow row) {

				if (row.getCell(molColIndex).isMissing()) {
					return DataType.getMissingCell();
				}
				CDKValue mol = (CDKValue) row.getCell(molColIndex);
				try {
					BitSet fingerprint;
					IAtomContainer con = CDKNodeUtils.getExplicitClone(mol.getAtomContainer());
					fingerprint = fp.getFingerprint(con);
					// transfer the bitset into a dense bit vector
					DenseBitVector bitVector = new DenseBitVector(fingerprint.size());
					for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
						bitVector.set(i);
					}
					DenseBitVectorCellFactory fact = new DenseBitVectorCellFactory(bitVector);
					return fact.createDataCell();
				} catch (Exception ex) {
					LOGGER.error("Error while creating fingerprints", ex);
					return DataType.getMissingCell();
				}
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.append(cf);
		return arranger;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger cr = createColumnRearranger(inData[0].getDataTableSpec());

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(inData[0], cr, exec) };
	}

	// /**
	// * @see org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel
	// * #prepareExecute(org.knime.core.data.DataTable[])
	// */
	// @Override
	// protected ExtendedCellFactory[] prepareExecute(final DataTable[] data)
	// throws Exception {
	// final int molColIndex = data[0].getDataTableSpec().findColumnIndex(
	// m_settings.molColumn());
	//
	// final DataColumnSpecCreator csc =
	// new DataColumnSpecCreator("fingerprint", BitVectorCell.TYPE);
	//
	// ExtendedCellFactory ecf = new ExtendedCellFactory() {
	// public DataCell[] getCells(final DataRow row) {
	// if (row.getCell(molColIndex).isMissing()) {
	// return new DataCell[]{DataType.getMissingCell()};
	// }
	// CDKCell mol = (CDKCell)row.getCell(molColIndex);
	// final Fingerprinter fp;
	// if (m_settings.extendedFingerprints()) {
	// fp = new ExtendedFingerprinter();
	// } else {
	// fp = new Fingerprinter();
	// }
	//
	// try {
	// BitSet fingerprint = fp.getFingerprint(mol.getMolecule());
	// return new DataCell[]{new BitVectorCell(fingerprint,
	// fingerprint.size())};
	// } catch (Exception ex) {
	// LOGGER.error("Error while creating fingerprints", ex);
	// return new DataCell[]{DataType.getMissingCell()};
	// }
	// }
	//
	// public ColumnDestination[] getColumnDestinations() {
	// return new ColumnDestination[] {new AppendColumn()};
	// }
	//
	// public DataColumnSpec[] getColumnSpecs() {
	// return new DataColumnSpec[] {csc.createSpec()};
	// }
	// };
	//
	// return new ExtendedCellFactory[] {ecf};
	// }
}
