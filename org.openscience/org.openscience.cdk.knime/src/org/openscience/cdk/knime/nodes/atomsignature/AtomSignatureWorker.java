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
package org.openscience.cdk.knime.nodes.atomsignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.knime.nodes.atomsignature.AtomSignatureSettings.AtomTypes;
import org.openscience.cdk.knime.nodes.atomsignature.AtomSignatureSettings.SignatureTypes;
import org.openscience.cdk.knime.preferences.CDKPreferencePage.NUMBERING;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.signature.AtomSignature;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * Multi threaded worker implementation for the Atom Signature Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class AtomSignatureWorker extends MultiThreadWorker<DataRow, List<DataRow>> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final int addNbColumns;
	private final AtomSignatureSettings settings;

	public AtomSignatureWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final BufferedDataContainer bdc, AtomSignatureSettings settings) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.columnIndex = columnIndex;
		this.bdc = bdc;
		this.settings = settings;

		addNbColumns = settings.isHeightSet() ? settings.getMaxHeight() - settings.getMinHeight() + 2 : 2;
	}

	@Override
	protected List<DataRow> compute(DataRow row, long index) throws Exception {

		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			DataCell[] outCells = new DataCell[addNbColumns];
			Arrays.fill(outCells, DataType.getMissingCell());
			List<DataRow> outRows = new ArrayList<DataRow>();
			outRows.add(new AppendedColumnRow(row, outCells));
			return outRows;
		}

		CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
		IAtomContainer molecule = cdkCell.getAtomContainer();

		if (settings.atomType().equals(AtomTypes.H)) {
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
		} else {
			molecule = SMSDNormalizer.convertExplicitToImplicitHydrogens(molecule);
		}

		int atomId = 1;
		Map<String, Integer> parentIdMap = new HashMap<String, Integer>();
		if (CDKNodePlugin.numbering() == NUMBERING.NONE || 
				CDKNodePlugin.numbering() == NUMBERING.SEQUENTIAL) {

			for (IAtom atom : molecule.atoms()) {
				parentIdMap.put(atom.getID(), atomId);
				atomId++;
			}
		}

		int count = 0;
		String parentId = "";
		Set<String> parentSet = new HashSet<String>();
		String atomType = settings.atomType().toString();

		HOSECodeGenerator hoseGen = new HOSECodeGenerator();
		List<DataRow> outRows = new ArrayList<DataRow>();

		// loop through the atoms and calculate the signatures
		if (atomType.equals("H")) {
			for (IAtom atom : molecule.atoms()) {

				if (atom.getSymbol().equals(AtomTypes.H.name())) {
					DataCell[] outCells = new DataCell[addNbColumns];
					String columnAtomId = "" + atom.getID();
					if (molecule.getConnectedAtomsList(atom).size() != 0)
						parentId = molecule.getConnectedAtomsList(atom).get(0).getID();
					else
						continue;
					columnAtomId = parentId;

					if (parentSet.contains(parentId)) {
						continue;
					}

					if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
						outCells[0] = new StringCell(columnAtomId);
					} else {
						outCells[0] = new StringCell("" + parentIdMap.get(parentId));
					}
					outCells = computeSignatures(atom, molecule, outCells, hoseGen);
					outRows.add(new AppendedColumnRow(new RowKey(row.getKey().getString() + "_" + count), row, outCells));
					parentSet.add(columnAtomId);
					count++;
				}
			}
		} else {

			for (IAtom atom : molecule.atoms()) {

				if (atom.getSymbol().equals(settings.atomType().name())) {
					DataCell[] outCells = new DataCell[addNbColumns];
					if (CDKNodePlugin.numbering() == NUMBERING.CANONICAL) {
						outCells[0] = new StringCell(atom.getID());
					} else {
						outCells[0] = new StringCell("" + parentIdMap.get(atom.getID()));
					}
					outCells = computeSignatures(atom, molecule, outCells, hoseGen);
					outRows.add(new AppendedColumnRow(new RowKey(row.getKey().getString() + "_" + count), row, outCells));
					count++;
				}
			}
		}
		return outRows;
	}

	private DataCell[] computeSignatures(final IAtom atom, IAtomContainer mol, DataCell[] outCells,
			HOSECodeGenerator hoseGen) throws CanceledExecutionException {

		for (int i = 1; i < outCells.length; i++) {
			String sign = null;
			if (settings.signatureType().equals(SignatureTypes.AtomSignatures)) {
				AtomSignature atomSignature = new AtomSignature(atom, (i - 1 + settings.getMinHeight()), mol);
				sign = atomSignature.toCanonicalString();
			} else if (settings.signatureType().equals(SignatureTypes.Hose)) {
				try {
					sign = hoseGen.getHOSECode(mol, atom, (i - 1 + settings.getMinHeight()));
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

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		List<DataRow> append = task.get();

		for (DataRow row : append) {
			bdc.addRowToTable(row);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
