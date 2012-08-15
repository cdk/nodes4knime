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
package org.openscience.cdk.knime.molprops;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Factory that generates molecular properties.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
final class MolPropsGenerator implements CellFactory {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(MolPropsGenerator.class);

	private final DataColumnSpec[] m_properties;
	private final String[] m_descClassNames;

	private final int m_smilesColIndex;

	/**
	 * Init this generator.
	 * 
	 * @param smilesIndex the index where to find the smiles cell
	 * @param classNames The internal identifiers for the descriptors
	 * @param props properties to generate
	 * @see MolPropsLibrary#getPropsDescription()
	 */
	MolPropsGenerator(final int smilesIndex, final String[] classNames, final DataColumnSpec[] props) {

		if (classNames.length != props.length) {
			throw new IndexOutOfBoundsException("Non matching lengths: " + classNames.length + " vs. " + props.length);
		}
		m_smilesColIndex = smilesIndex;
		m_descClassNames = classNames;
		m_properties = props;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(final DataRow row) {

		DataCell sCell = row.getCell(m_smilesColIndex);
		DataCell[] newCells = new DataCell[m_properties.length];
		if (sCell.isMissing()) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}
		if (!(sCell instanceof CDKValue)) {
			throw new IllegalArgumentException("No CDK cell at " + m_smilesColIndex + ": " + sCell.getClass().getName());
		}
		IAtomContainer mol = null;
		try {
			mol = CDKNodeUtils.getExplicitClone(((CDKValue) sCell).getAtomContainer());
		} catch (Exception exception) {
			LOGGER.debug("Unable to parse molecule in row \"" + row.getKey() + "\"", exception);
		}

		for (int i = 0; i < m_descClassNames.length; i++) {
			String prop = m_descClassNames[i];
			if (prop.equals("molecularformula")) {
				IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(mol);
				newCells[i] = new StringCell(MolecularFormulaManipulator.getString(formula));
			} else if (prop.equals("heavyatoms")) {
				newCells[i] = new IntCell(AtomContainerManipulator.getHeavyAtoms(mol).size());
			} else if (prop.equals("molarmass")) {
				IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(mol);
				newCells[i] = new DoubleCell(MolecularFormulaManipulator.getNaturalExactMass(formula));
			} else if (prop.equals("spthreechar")) {
				double character = getSp3Character(mol);
				newCells[i] = character == -1 ? DataType.getMissingCell() : new DoubleCell(character);
			} else {
				newCells[i] = MolPropsLibrary.getProperty(row.getKey().toString(), mol, prop);
			}
		}
		return newCells;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return m_properties;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec) {

		exec.setProgress(curRowNr / (double) rowCount, "Calculated properties for row " + curRowNr + " (\"" + lastKey
				+ "\")");
	}
	
	private double getSp3Character(IAtomContainer mol) {
		
		double sp3 = 0;
		for (IAtom atom : mol.atoms()) {

			if (!atom.getSymbol().equals("C")) continue;
			
			if (atom.getHybridization() == IAtomType.Hybridization.SP3) sp3++;
		}
		
		return sp3 / mol.getAtomCount();
	}
}
