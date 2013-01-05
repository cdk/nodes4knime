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
package org.openscience.cdk.knime.molprops;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

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
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class MolPropsNodeModel extends ThreadedColAppenderNodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(MolPropsNodeModel.class);

	/** NodeSettings file containing names. */
	private static final String MOLPROPS_IDENTIFIER_FILE = MolPropsNodeModel.class.getPackage().getName()
			.replace('.', '/')
			+ "/molprops.set";

	private static final Map<DataColumnSpec, String> MOLPROPS_IDENTIFIER_MAP;

	/** NodeSettings key to store all property desriptions. */
	protected static final String CFGKEY_PROPS = "PropertyDescriptions";

	/** NodeSettings key to store smiles column header. */
	protected static final String CFGKEY_SMILES = "smilesIndex";

	private final ArrayList<String> m_propDescriptions;

	private String m_cdkColumn;

	static {
		// available properties for this node
		LinkedHashSet<String> descResultSet = new LinkedHashSet<String>();
		try {
			ClassLoader loader = MolPropsNodeModel.class.getClassLoader();
			InputStream stream = loader.getResourceAsStream(MOLPROPS_IDENTIFIER_FILE);
			BufferedReader in = new BufferedReader(new InputStreamReader(stream));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					descResultSet.add(line);
				}
			}
		} catch (Exception ioe) {
			LOGGER.debug("Failed load descriptor result file " + MOLPROPS_IDENTIFIER_FILE, ioe);
		}
		LinkedHashMap<DataColumnSpec, String> specStringMap = new LinkedHashMap<DataColumnSpec, String>();
		for (String className : descResultSet) {
			DataColumnSpec s = MolPropsLibrary.getColumnSpec(className);
			if (s == null) {
				LOGGER.debug("Descriptor \"" + className + "\" not available");
			} else {
				specStringMap.put(s, className);
			}
		}
		// #########################
		// custom "non-qsar" methods
		DataColumnSpec mfSpec = new DataColumnSpecCreator("Molecular Formula", StringCell.TYPE).createSpec();
		DataColumnSpec haSpec = new DataColumnSpecCreator("No. of Heavy Atoms", IntCell.TYPE).createSpec();
		DataColumnSpec mmSpec = new DataColumnSpecCreator("Molar Mass", DoubleCell.TYPE).createSpec();
		DataColumnSpec spSpec = new DataColumnSpecCreator("SP3 Character", DoubleCell.TYPE).createSpec();
		specStringMap.put(mfSpec, "molecularformula");
		specStringMap.put(haSpec, "heavyatoms");
		specStringMap.put(mmSpec, "molarmass");
		specStringMap.put(spSpec, "spthreechar");
		MOLPROPS_IDENTIFIER_MAP = Collections.unmodifiableMap(specStringMap);
		// #########################
	}

	/** Constructor: One input, one output. */
	public MolPropsNodeModel() {

		super(1, 1);
		setMaxThreads(CDKNodeUtils.getMaxNumOfThreads());
		m_propDescriptions = new ArrayList<String>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString(CFGKEY_SMILES, m_cdkColumn);
		String[] props = m_propDescriptions.toArray(new String[0]);
		settings.addStringArray(CFGKEY_PROPS, props);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		settings.getString(CFGKEY_SMILES);
		settings.getStringArray(CFGKEY_PROPS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_cdkColumn = settings.getString(CFGKEY_SMILES);
		String[] props = settings.getStringArray(CFGKEY_PROPS);
		m_propDescriptions.clear();
		m_propDescriptions.addAll(Arrays.asList(props));
	}

	private String[] propsClassNames;
	private DataColumnSpec[] propsSpec;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int colIndex = data[0].getDataTableSpec().findColumnIndex(m_cdkColumn);

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {

				DataCell sCell = row.getCell(colIndex);
				DataCell[] newCells = new DataCell[propsSpec.length];
				if (sCell.isMissing()) {
					Arrays.fill(newCells, DataType.getMissingCell());
					return newCells;
				}
				if (!(sCell instanceof CDKValue)) {
					throw new IllegalArgumentException("No CDK cell at " + colIndex + ": " + sCell.getClass().getName());
				}
				IAtomContainer mol = null;
				try {
					mol = CDKNodeUtils.getExplicitClone(((CDKValue) sCell).getAtomContainer());
				} catch (Exception exception) {
					LOGGER.debug("Unable to parse molecule in row \"" + row.getKey() + "\"", exception);
				}

				for (int i = 0; i < propsClassNames.length; i++) {
					String prop = propsClassNames[i];
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
						Object[] params = new Object[0];
						if (prop.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor")) {
							params = new Object[] { new Boolean(false), new Boolean(false) };
						} else if (prop
								.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.RuleOfFiveDescriptor")) {
							params = new Object[] { new Boolean(false) };
						} else if (prop
								.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.BCUTDescriptor")) {
							params = new Object[] { 0, 0.25, new Boolean(false) };
						} else if (prop
								.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.HBondAcceptorCountDescriptor")) {
							params = new Object[] { new Boolean(false) };
						} else if (prop
								.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.HBondDonorCountDescriptor")) {
							params = new Object[] { new Boolean(false) };
						} else if (prop
								.equalsIgnoreCase("org.openscience.cdk.qsar.descriptors.molecular.RotatableBondsCountDescriptor")) {
							params = new Object[] { new Boolean(true) };
						}
						newCells[i] = MolPropsLibrary.getProperty(row.getKey().toString(), mol, prop, params);
					}
				}
				return newCells;
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				ColumnDestination[] cd;
				try {
					cd = new ColumnDestination[generateOutputColSpec(data[0].getDataTableSpec()).length];
					Arrays.fill(cd, new AppendColumn());
				} catch (InvalidSettingsException exception) {
					return new ColumnDestination[] { new AppendColumn() };
				}
				return cd;
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				try {
					return generateOutputColSpec(data[0].getDataTableSpec());
				} catch (InvalidSettingsException exception) {
					return null;
				}
			}

			private double getSp3Character(IAtomContainer mol) {

				double sp3 = 0;
				for (IAtom atom : mol.atoms()) {

					if (!atom.getSymbol().equals("C"))
						continue;

					if (atom.getHybridization() == IAtomType.Hybridization.SP3)
						sp3++;
				}

				return sp3 / mol.getAtomCount();
			}
		};

		return new ExtendedCellFactory[] { cf };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		DataTableSpec inSpec = inSpecs[0];
		if (!inSpec.containsCompatibleType(CDKValue.class)) {
			throw new InvalidSettingsException("No CDK cell in input table");
		}
		if (m_cdkColumn == null) {
			int cdkCellCount = 0;
			String cdkColName = null;
			for (int i = 0; i < inSpec.getNumColumns(); i++) {
				DataType cC = inSpec.getColumnSpec(i).getType();
				if (cC.isCompatible(CDKValue.class)) {
					cdkCellCount++;
					cdkColName = inSpec.getColumnSpec(i).getName();
				}
			}
			assert (cdkCellCount >= 1);
			if (cdkCellCount == 1) {
				LOGGER.info("No CDK cell was set: I fix it to \"" + cdkColName + "\".");
				m_cdkColumn = cdkColName;
			} else {
				throw new InvalidSettingsException("No CDK cell defined");
			}
		}
		int cdkCol = inSpec.findColumnIndex(m_cdkColumn);
		if (cdkCol < 0) {
			throw new InvalidSettingsException("No CDK column \"" + m_cdkColumn + "\" in table.");
		}
		DataTableSpec outSpec = new DataTableSpec(generateOutputColSpec(inSpec));
		DataTableSpec[] outSpecs = new DataTableSpec[] { new DataTableSpec(inSpec, outSpec) };
		return outSpecs;
	}

	private DataColumnSpec[] generateOutputColSpec(final DataTableSpec spec) throws InvalidSettingsException {

		HashSet<String> hash = new HashSet<String>(m_propDescriptions);
		propsClassNames = new String[m_propDescriptions.size()];
		int index = 0;
		for (Map.Entry<DataColumnSpec, String> entry : MOLPROPS_IDENTIFIER_MAP.entrySet()) {
			if (hash.remove(entry.getKey().getName())) {
				propsClassNames[index++] = entry.getValue();
			}
		}
		if (!hash.isEmpty()) {
			throw new InvalidSettingsException("Some properties are unknown: " + Arrays.toString(hash.toArray()));
		}
		assert index == propsClassNames.length;
		// MolPropsGenerator needs the column specs of the new columns, we need to generate that
		propsSpec = new DataColumnSpec[propsClassNames.length];
		for (int i = 0; i < propsClassNames.length; i++) {
			String s = propsClassNames[i];
			DataColumnSpec colSpec = MolPropsLibrary.getColumnSpec(s);
			if (spec.containsName(colSpec.getName())) {
				int uniquifier = 1;
				String name;
				do {
					name = colSpec.getName() + " #" + uniquifier;
					uniquifier++;
				} while (spec.containsName(name));
				DataColumnSpecCreator c = new DataColumnSpecCreator(colSpec);
				c.setName(name);
				colSpec = c.createSpec();
			}
			propsSpec[i] = colSpec;
		}
		return propsSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do here
	}

	/**
	 * Get list of available descriptors for this node.
	 * 
	 * @return This list.
	 */
	static DataColumnSpec[] getAvailableDescriptorList() {

		return MOLPROPS_IDENTIFIER_MAP.keySet().toArray(new DataColumnSpec[0]);
	}
}
