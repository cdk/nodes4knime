/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.elementfilter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Multi threaded worker implementation for the Element Filter Node.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class ElementFilterWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final Set<Long> matchedRows;
	private final BufferedDataContainer[] bdcs;
	private final Set<String> elementSet;
	private final boolean keep;

	public ElementFilterWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final ElementFilterSettings settings, final BufferedDataContainer... bdcs) {

		super(maxQueueSize, maxActiveInstanceSize);

		String[] elements = settings.getElements().split(",");
		elementSet = new HashSet<String>();
		for (String element : elements) {
			elementSet.add(element);
		}
		this.keep = settings.getKeep();

		this.exec = exec;
		this.bdcs = bdcs;
		this.columnIndex = columnIndex;
		this.matchedRows = Collections.synchronizedSet(new HashSet<Long>());
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			// fall through
		} else {
			CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
			IAtomContainer mol = cdkCell.getAtomContainer();
			IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(mol);
			List<IElement> sumElements = MolecularFormulaManipulator.getHeavyElements(formula);
			
			// keep CHNOPS
			if (keep) {
				boolean isValid = true;
				for (IElement element : sumElements) {
					String symbol = element.getSymbol();
					if (!elementSet.contains(symbol)) {
						isValid = false;
						break;
					}
				}
				if (isValid) {
					matchedRows.add(index);
				}
		    // remove everything else
			} else {
				boolean isValid = true;
				for (IElement element : sumElements) {
					String symbol = element.getSymbol();
					if (elementSet.contains(symbol)) {
						isValid = false;
						break;
					}
				}
				if (isValid) {
					matchedRows.add(index);
				}
			}
		}
		
		return row;
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		long index = task.getIndex();
		DataRow replace = task.get();
		if (matchedRows.contains(index)) {
			bdcs[0].addRowToTable(replace);
		} else {
			bdcs[1].addRowToTable(replace);
		}

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
