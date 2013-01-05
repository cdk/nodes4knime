/*
 * Copyright (c) 2013, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
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
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class AtomSignatureNodeModel extends ThreadedTableBuilderNodeModel {

	private final AtomSignatureSettings m_settings = new AtomSignatureSettings();
	private int molColIndex;
	private int addNbColumns;

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
	protected DataTableSpec[] prepareExecute(final DataTable[] data) throws Exception {

		molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());
		addNbColumns = m_settings.isHeightSet() ? m_settings.getMaxHeight() - m_settings.getMinHeight() + 2 : 2;

		DataColumnSpec[] clmspecs = configSpecs(data[0].getDataTableSpec());

		return new DataTableSpec[] { new DataTableSpec(clmspecs) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processRow(final DataRow inRow, final BufferedDataTable[] additionalData,
			final RowAppender[] outputTables) throws Exception {

		// check if cell is missing and return a missing value in that case
		if (inRow.getCell(molColIndex).isMissing()) {
			DataCell[] missings = new DataCell[addNbColumns];
			Arrays.fill(missings, DataType.getMissingCell());
			outputTables[0].addRowToTable(new AppendedColumnRow(inRow, missings));
		} else {
			IAtomContainer mol = ((CDKValue) inRow.getCell(molColIndex)).getAtomContainer();

			if (m_settings.atomType().equals(AtomTypes.H)) {
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
			} else {
				mol = SMSDNormalizer.convertExplicitToImplicitHydrogens(mol);
			}

			int atomId = 1;
			Map<String, Integer> parentIdMap = new HashMap<String, Integer>();
			if (CDKNodePlugin.numbering() == NUMBERING.SEQUENTIAL) {

				for (IAtom atom : mol.atoms()) {
					parentIdMap.put(atom.getID(), atomId);
					atomId++;
				}
			}

			int count = 0;
			String parentId = "";
			Set<String> parentSet = new HashSet<String>();
			String atomType = m_settings.atomType().toString();

			HOSECodeGenerator hoseGen = new HOSECodeGenerator();
			
			// loop through the atoms and calculate the signatures
			if (atomType.equals("H")) {

				for (IAtom atom : mol.atoms()) {

					if (atom.getSymbol().equals(AtomTypes.H.name())) {
						String columnAtomId = "" + atom.getID();
						if (mol.getConnectedAtomsList(atom).size() != 0)
							parentId = mol.getConnectedAtomsList(atom).get(0).getID();
						else
							continue;
						columnAtomId = parentId;

						if (parentSet.contains(parentId)) {
							continue;
						}

						DataCell[] outCells = new DataCell[addNbColumns];
						if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
							outCells[0] = new StringCell(columnAtomId);
						} else {
							outCells[0] = new StringCell("" + parentIdMap.get(parentId));
						}
						outCells = computeSignatures(atom, mol, outCells, hoseGen);
						outputTables[0].addRowToTable(new AppendedColumnRow(new RowKey(inRow.getKey().getString() + "_"
								+ count), inRow, outCells));
						parentSet.add(columnAtomId);
						count++;
					}
				}
			} else {

				for (IAtom atom : mol.atoms()) {

					if (atom.getSymbol().equals(m_settings.atomType().name())) {
						DataCell[] outCells = new DataCell[addNbColumns];
						if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
							outCells[0] = new StringCell(atom.getID());
						} else {
							outCells[0] = new StringCell("" + parentIdMap.get(atom.getID()));
						}
						outCells = computeSignatures(atom, mol, outCells, hoseGen);
						outputTables[0].addRowToTable(new AppendedColumnRow(new RowKey(inRow.getKey().getString() + "_"
								+ count), inRow, outCells));
						count++;
					}
				}
			}
		}
	}

	private DataCell[] computeSignatures(final IAtom atom, IAtomContainer mol, DataCell[] outCells, HOSECodeGenerator hoseGen)
			throws CanceledExecutionException {

		for (int i = 1; i < outCells.length; i++) {
			String sign = null;
			if (m_settings.signatureType().equals(SignatureTypes.AtomSignatures)) {
				AtomSignature atomSignature = new AtomSignature(atom, (i + m_settings.getMinHeight()), mol);
				sign = atomSignature.toCanonicalString();
			} else if (m_settings.signatureType().equals(SignatureTypes.Hose)) {
				try {
					sign = hoseGen.getHOSECode(mol, atom, (i + m_settings.getMinHeight()));
				} catch (CDKException e) {
					// do nothing
				}
			}

			if (sign != null)
				outCells[i] = new StringCell(sign);
			else
				outCells[i] = DataType.getMissingCell();
		}
		return outCells;
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

		DataColumnSpec[] newCs = configSpecs(inSpecs[0]);

		return new DataTableSpec[] { new DataTableSpec(newCs) };
	}

	private DataColumnSpec[] configSpecs(final DataTableSpec inSpecs) {

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
