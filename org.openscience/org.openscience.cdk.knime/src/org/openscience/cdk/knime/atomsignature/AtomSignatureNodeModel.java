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
package org.openscience.cdk.knime.atomsignature;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.knime.atomsignature.AtomSignatureSettings.AtomTypes;
import org.openscience.cdk.knime.atomsignature.AtomSignatureSettings.SignatureTypes;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.signature.AtomSignature;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model implementation of AtomSignature.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 */
public class AtomSignatureNodeModel extends NodeModel {

	private final AtomSignatureSettings m_settings = new AtomSignatureSettings();

	/**
	 * Constructor for the node model.
	 */
	protected AtomSignatureNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		// get the index of the molcolumn
		final int molColIndex = inData[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());

		// declare the column to be rearranged
		int addNbColumns = m_settings.isHeightSet() ? m_settings.getMaxHeight() - m_settings.getMinHeight() + 2 : 2;

		DataColumnSpec[] clmspecs = configCSpecs(inData[0].getDataTableSpec());

		DataTableSpec tbspecs = new DataTableSpec(clmspecs);

		BufferedDataContainer container = exec.createDataContainer(tbspecs);

		for (DataRow inRow : inData[0]) {
			DataCell[] inCells = new DataCell[inRow.getNumCells()];

			for (int i = 0; i < inCells.length; i++) {
				inCells[i] = inRow.getCell(i);
			}
			// check if cell is missing and return a missing value in that case
			if (inRow.getCell(molColIndex).isMissing()) {
				DataCell[] newRow = new DataCell[inCells.length + addNbColumns];
				DataCell[] missings = new DataCell[addNbColumns];
				Arrays.fill(missings, DataType.getMissingCell());
				System.arraycopy(inCells, 0, newRow, 0, inCells.length);
				System.arraycopy(missings, 0, newRow, inCells.length, missings.length);
				container.addRowToTable(new DefaultRow(inRow.getKey(), newRow));
			} else {
				// declare the iatomcontainer using the information in the molcell
				IAtomContainer mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();

				IAtomContainer molTmp = (IAtomContainer) mol.clone();
				if (m_settings.atomType().equals(AtomTypes.H)) {
					AtomContainerManipulator.convertImplicitToExplicitHydrogens(molTmp);
				} else {
					molTmp = SMSDNormalizer.convertExplicitToImplicitHydrogens(molTmp);
				}

				int atomId = 1;
				Map<String, Integer> parentIdMap = new HashMap<String, Integer>();
				if (CDKNodePlugin.numbering() == NUMBERING.SEQUENTIAL) {

					for (IAtom atom : molTmp.atoms()) {
						parentIdMap.put(atom.getID(), atomId);
						atomId++;
					}
				}

				int contSize = container.size();
				int count = 0;
				String parentId = "";
				Set<String> parentSet = new HashSet<String>();
				String atomType = m_settings.atomType().toString();

				// loop through the atoms and calculate the signatures
				if (atomType.equals("H")) {

					for (IAtom atom : molTmp.atoms()) {

						if (atom.getSymbol().equals(AtomTypes.H.name())) {
							String columnAtomId = "" + atom.getID();
							if (molTmp.getConnectedAtomsList(atom).size() != 0)
								parentId = molTmp.getConnectedAtomsList(atom).get(0).getID();
							else 
								continue;
							columnAtomId = parentId;

							if (parentSet.contains(parentId)) {
								continue;
							}

							// create a new row
							DataCell[] newRow = new DataCell[inCells.length + addNbColumns];
							// copy cells from the input row to the new row
							System.arraycopy(inCells, 0, newRow, 0, inCells.length);
							DataCell[] atomCanonicalNb = null;
							if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
								atomCanonicalNb = new DataCell[] { new StringCell(columnAtomId) };
							} else {
								atomCanonicalNb = new DataCell[] { new StringCell("" + parentIdMap.get(parentId)) };
							}
							System.arraycopy(atomCanonicalNb, 0, newRow, inCells.length, 1);
							DataCell[] signatures = computeSignatures(atom, molTmp, addNbColumns - 1);
							System.arraycopy(signatures, 0, newRow, inCells.length + 1, signatures.length);
							container.addRowToTable(new DefaultRow(inRow.getKey() + "_" + Integer.toString(count),
									newRow));
							parentSet.add(columnAtomId);
							count++;
						}
					}
				} else {

					for (IAtom atom : molTmp.atoms()) {

						if (atom.getSymbol().equals(m_settings.atomType().name())) {
							// create a new row
							DataCell[] newRow = new DataCell[inCells.length + addNbColumns];
							// copy cells from the input row to the new row
							System.arraycopy(inCells, 0, newRow, 0, inCells.length);
							DataCell[] atomCanonicalNb = null;
							if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
								atomCanonicalNb = new DataCell[] { new StringCell(atom.getID()) };
							} else {
								atomCanonicalNb = new DataCell[] { new StringCell("" + parentIdMap.get(atom.getID())) };
							}
							System.arraycopy(atomCanonicalNb, 0, newRow, inCells.length, 1);
							DataCell[] signatures = computeSignatures(atom, molTmp, addNbColumns - 1);
							System.arraycopy(signatures, 0, newRow, inCells.length + 1, signatures.length);
							container.addRowToTable(new DefaultRow(inRow.getKey() + "_" + Integer.toString(count),
									newRow));
							count++;
						}
					}
				}
				if (contSize == container.size()) {
					DataCell[] newRow = new DataCell[inCells.length + addNbColumns];
					DataCell[] missings = new DataCell[addNbColumns];
					Arrays.fill(missings, DataType.getMissingCell());
					System.arraycopy(inCells, 0, newRow, 0, inCells.length);
					System.arraycopy(missings, 0, newRow, inCells.length, missings.length);
					container.addRowToTable(new DefaultRow(inRow.getKey(), newRow));
				}
			}
		}

		container.close();

		return new BufferedDataTable[] { container.getTable() };

	}

	private DataCell[] computeSignatures(final IAtom atom, IAtomContainer mol, int addColms)
			throws CanceledExecutionException {

		DataCell[] signatures = new DataCell[addColms];

		for (int i = 0; i < signatures.length; i++) {
			String sign = null;
			if (m_settings.signatureType().equals(SignatureTypes.AtomSignatures)) {
				AtomSignature atomSignature = new AtomSignature(atom, (i + m_settings.getMinHeight()), mol);
				sign = atomSignature.toCanonicalString();
			} else if (m_settings.signatureType().equals(SignatureTypes.Hose)) {
				HOSECodeGenerator hoseGen = new HOSECodeGenerator();

				try {
					sign = hoseGen.getHOSECode(mol, atom, (i + m_settings.getMinHeight()));
				} catch (CDKException e) {
					// do nothing
				}
			}

			if (sign != null)
				signatures[i] = new StringCell(sign);
			else
				signatures[i] = DataType.getMissingCell();
		}
		return signatures;
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
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// get the index of the molecule column
		int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
		// if molcolumn does not exist
		if (molCol == -1) {
			// iterate through the tablespecs to find where the molcolumn is
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
			// if index of molcolumn exists
			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				// store column name in the settings
				m_settings.molColumnName(name);
			}
		}

		// if there is no molcolumn at this point complaint
		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColumnName() + "' does not exist");
		}

		DataColumnSpec[] newCs = configCSpecs(inSpecs[0]);

		return new DataTableSpec[] { new DataTableSpec(newCs) };
	}

	private DataColumnSpec[] configCSpecs(final DataTableSpec inSpecs) {

		// add a new column to the specs...
		int inNbColumns = inSpecs.getNumColumns();
		int addNbColumns = m_settings.isHeightSet() ? m_settings.getMaxHeight() - m_settings.getMinHeight() + 2 : 2;

		DataColumnSpec[] cs = new DataColumnSpec[inNbColumns + addNbColumns];

		// copy the columnspecs from the previous table
		for (int i = 0; i < inNbColumns; i++)
			cs[i] = inSpecs.getColumnSpec(i);
		// add the columnspecs for the new columns
		for (int i = inNbColumns; i < (inNbColumns + addNbColumns); i++) {
			String name = null;
			if (i == inNbColumns) {
				name = "Atom ID";
			} else {
				// Check if the name we want for the new column already exists and if so generate a different one
				name = m_settings.signatureType().equals(SignatureTypes.AtomSignatures) ? DataTableSpec
						.getUniqueColumnName(inSpecs, "Signature " + (i - 1 + m_settings.getMinHeight() - inNbColumns))
						: DataTableSpec.getUniqueColumnName(inSpecs, "HOSE "
								+ (i - 1 + m_settings.getMinHeight() - inNbColumns));
			}
			cs[i] = new DataColumnSpecCreator(name, StringCell.TYPE).createSpec();
		}
		return cs;
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_settings.loadSettings(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		AtomSignatureSettings s = new AtomSignatureSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
		if (m_settings.isHeightSet()) {
			Integer minHeight = m_settings.getMinHeight();
			Integer maxHeight = m_settings.getMaxHeight();
			if (minHeight == null || maxHeight == null || minHeight > maxHeight) {
				throw new InvalidSettingsException("Heights wrongly defined");
			}
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
