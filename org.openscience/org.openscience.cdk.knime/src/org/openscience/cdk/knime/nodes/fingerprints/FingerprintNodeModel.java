/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.openscience.cdk.knime.nodes.fingerprints;

import java.util.BitSet;

import org.knime.core.data.AdapterValue;
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
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.knime.nodes.fingerprints.FingerprintSettings.FingerprintTypes;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model for the fingerprint node. It uses the CDK to create fingerprints (which are essentially bit sets)
 * for the molecules in the input table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 * 
 */
public class FingerprintNodeModel extends CDKNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(FingerprintNodeModel.class);

	/**
	 * Creates a new model for the fingerprint node.
	 */
	public FingerprintNodeModel() {
		super(1, 1, new FingerprintSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		columnIndex = spec.findColumnIndex(settings.targetColumn());
		
		final IFingerprinter fp;
		FingerprintTypes fpType = settings(FingerprintSettings.class).fingerprintType();
		if (fpType.equals(FingerprintTypes.Extended)) {
			fp = new ExtendedFingerprinter();
		} else if (fpType.equals(FingerprintTypes.EState)) {
			fp = new EStateFingerprinter();
		} else if (fpType.equals(FingerprintTypes.Pubchem)) {
			fp = new PubchemFingerprinter();
		} else if (fpType.equals(FingerprintTypes.MACCS)) {
			fp = new MACCSFingerprinter();
		} else {
			fp = new Fingerprinter();
		}

		String newColName = settings(FingerprintSettings.class).fingerprintType() + " fingerprints for "
				+ settings.targetColumn();
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);

		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DenseBitVectorCell.TYPE);
		DataColumnSpec appendSpec = c.createSpec();
		
		SingleCellFactory cf = new SingleCellFactory(true, appendSpec) {

			@Override
			public DataCell getCell(final DataRow row) {
				
				if (row.getCell(columnIndex).isMissing()
						|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
					return DataType.getMissingCell();
				}
				
				CDKValue mol = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
				try {
					BitSet fingerprint;
					IAtomContainer con = CDKNodeUtils.getExplicitClone(mol.getAtomContainer());
					fingerprint = fp.getBitFingerprint(con).asBitSet();
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
		arranger.ensureColumnIsConverted(CDKTypeConverter.createConverter(spec, columnIndex), columnIndex);
		arranger.append(cf);
		return arranger;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		FingerprintSettings tmpSettings = new FingerprintSettings();
		tmpSettings.loadSettings(settings);
		
		if ((tmpSettings.targetColumn() == null) || (tmpSettings.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No compatible molecule column chosen");
		}
	}
}
