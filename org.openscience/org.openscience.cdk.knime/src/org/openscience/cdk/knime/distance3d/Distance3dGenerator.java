/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.distance3d;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ExecutionMonitor;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.similarity.DistanceMoment;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Distance3dGenerator implements CellFactory {

	private final int molColIndex;
	private final DataColumnSpec[] dataColumnSpec;

	/**
	 * Constructs the cell factory.
	 * 
	 * @param dataColumnSpec the data column output specification
	 * @param molColIndex the CDK molecule column index
	 */
	public Distance3dGenerator(int molColIndex, DataColumnSpec[] dataColumnSpec) {

		this.molColIndex = molColIndex;
		this.dataColumnSpec = dataColumnSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(DataRow row) {

		DataCell cdkCell = row.getCell(molColIndex);
		DataCell[] momentCells = new DataCell[dataColumnSpec.length];

		if (cdkCell.isMissing()) {
			Arrays.fill(momentCells, DataType.getMissingCell());
			return momentCells;
		}

		checkIsCdkCell(cdkCell);

		IAtomContainer molecule = ((CDKValue) row.getCell(molColIndex)).getAtomContainer();
		if (!ConnectivityChecker.isConnected(molecule))
			molecule = ConnectivityChecker.partitionIntoMolecules(molecule).getAtomContainer(0);
		IAtomContainer moleculeMinusH = AtomContainerManipulator.removeHydrogens(molecule);
		
		try {
			float[] moments = DistanceMoment.generateMoments(moleculeMinusH);

			int i = 0;
			for (float moment : moments) {
				momentCells[i] = new DoubleCell(moment);
				i++;
			}
		} catch (CDKException exception) {
			Arrays.fill(momentCells, DataType.getMissingCell());
		}

		return momentCells;
	}

	private void checkIsCdkCell(DataCell dataCell) {

		if (!(dataCell instanceof CDKValue)) {
			throw new IllegalArgumentException("No CDK cell at " + dataCell + ": " + dataCell.getClass().getName());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return dataColumnSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {

		exec.setProgress(curRowNr / (double) rowCount, "Calculated WHIM for row " + curRowNr + " (\"" + lastKey + "\")");
	}
}
