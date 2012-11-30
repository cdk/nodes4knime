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
package org.openscience.cdk.knime.rmsdcalculator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.distmatrix.type.DistanceVectorDataCell;
import org.knime.distmatrix.type.DistanceVectorDataCellFactory;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.AtomMappingTools;
import org.openscience.cdk.knime.rmsdcalculator.RMSDCalculatorSettings.AlignmentTypes;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model implementation of RMSDCalculator.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class RMSDCalculatorNodeModel extends ThreadedColAppenderNodeModel {

	private final RMSDCalculatorSettings m_settings = new RMSDCalculatorSettings();
	// the logger instance
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RMSDCalculatorNodeModel.class);

	/**
	 * Constructor for the node model.
	 */
	protected RMSDCalculatorNodeModel() {

		super(1, 1);
		// builds a pairwise similarity matrix -> max. 1 thread to ensure sync
		setMaxThreads(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {

		final int molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());
		final List<IAtomContainer> molList = new ArrayList<IAtomContainer>();

		ExtendedCellFactory cf = new ExtendedCellFactory() {

			@Override
			public DataCell[] getCells(final DataRow row) {
				
				if (row.getCell(molColIndex).isMissing()) {
					return new DataCell[] { DataType.getMissingCell() };
				}
				CDKValue mol = (CDKValue) row.getCell(molColIndex);
				try {
					List<DoubleCell> rmsds = new ArrayList<DoubleCell>();
					IAtomContainer con = (IAtomContainer) mol.getAtomContainer();

					if (!GeometryTools.has3DCoordinates(con)) {
						return new DataCell[] { DataType.getMissingCell() };
					}

					for (int i = 0; i < molList.size(); i++) {
						if (m_settings.alignmentType().equals(AlignmentTypes.Isomorphic)) {
							Map<Integer, Integer> map = new HashMap<Integer, Integer>();
							if (molList.get(i).getAtomCount() > con.getAtomCount()) {
								map = AtomMappingTools.mapAtomsOfAlignedStructures(molList.get(i), con, map);
								rmsds.add(new DoubleCell(GeometryTools.getAllAtomRMSD(molList.get(i), con, map, true)));
							} else {
								map = AtomMappingTools.mapAtomsOfAlignedStructures(con, molList.get(i), map);
								rmsds.add(new DoubleCell(GeometryTools.getAllAtomRMSD(con, molList.get(i), map, true)));
							}
						} else {
							KabschAlignment sa = new KabschAlignment(con, molList.get(i));
							sa.align();
							rmsds.add(new DoubleCell(sa.getRMSD()));
						}
					}
					molList.add(con);
					double[] rmsdsD = new double[rmsds.size()];
					for (int i = 0; i < rmsds.size(); i++) {
						rmsdsD[i] = rmsds.get(i).getDoubleValue();
					}

					return new DataCell[] { DistanceVectorDataCellFactory.createCell(rmsdsD, rmsdsD.length) };
				} catch (Exception ex) {
					ex.printStackTrace();
					System.out.println("ERR:" + ex.getMessage());
					LOGGER.error("Error while calculating RMSD", ex);
					return new DataCell[] { DataType.getMissingCell() };
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {

				return new ColumnDestination[] { new AppendColumn() };
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {

				return new DataColumnSpec[] { new DataColumnSpecCreator("RMSD", DistanceVectorDataCell.TYPE)
						.createSpec() };
			}
		};

		return new ExtendedCellFactory[] { cf };
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

		if (m_settings.molColumnName() == null) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isCompatible(CDKValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.molColumnName(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else {
				throw new InvalidSettingsException("No CDK compatible column in input table");
			}
		}

		String newColName = DataTableSpec.getUniqueColumnName(inSpecs[0], "RMSD");
		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DistanceVectorDataCell.TYPE);

		DataTableSpec dts = new DataTableSpec(inSpecs[0], new DataTableSpec(c.createSpec()));
		return new DataTableSpec[] { dts };
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

		RMSDCalculatorSettings s = new RMSDCalculatorSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
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
