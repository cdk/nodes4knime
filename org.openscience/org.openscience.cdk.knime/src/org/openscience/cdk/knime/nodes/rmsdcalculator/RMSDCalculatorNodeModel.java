/*
 * Copyright (c) 2016, Luis Filipe de Figueiredo (ldpf@ebi.ac.uk). All rights reserved.
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
package org.openscience.cdk.knime.nodes.rmsdcalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.distmatrix.type.DistanceVectorDataCell;
import org.knime.distmatrix.type.DistanceVectorDataCellFactory;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.AtomMappingTools;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.knime.nodes.rmsdcalculator.RMSDCalculatorSettings.AlignmentTypes;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model implementation of RMSDCalculator.
 * 
 * @author Luis Filipe de Figueiredo, European Bioinformatics Institute
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class RMSDCalculatorNodeModel extends CDKNodeModel {

	// the logger instance
	private static final NodeLogger LOGGER = NodeLogger.getLogger(RMSDCalculatorNodeModel.class);

	/**
	 * Constructor for the node model.
	 */
	protected RMSDCalculatorNodeModel() {
		super(1, 1, new RMSDCalculatorSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {

		String newColName = DataTableSpec.getUniqueColumnName(spec, "RMSD");
		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName, DistanceVectorDataCell.TYPE);

		final List<IAtomContainer> molList = new ArrayList<IAtomContainer>();

		// builds a pairwise similarity matrix -> max. 1 thread to ensure sync
		SingleCellFactory cf = new SingleCellFactory(false, c.createSpec()) {

			@Override
			public DataCell getCell(final DataRow row) {

				if (row.getCell(columnIndex).isMissing()
						|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
					return DataType.getMissingCell();
				}

				CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
				IAtomContainer m = cdkCell.getAtomContainer();

				try {
					List<DoubleCell> rmsds = new ArrayList<DoubleCell>();

					if (!GeometryTools.has3DCoordinates(m)) {
						return DataType.getMissingCell();
					}

					for (int i = 0; i < molList.size(); i++) {
						if (settings(RMSDCalculatorSettings.class).alignmentType().equals(AlignmentTypes.Isomorphic)) {
							Map<Integer, Integer> map = new HashMap<Integer, Integer>();
							if (molList.get(i).getAtomCount() > m.getAtomCount()) {
								map = AtomMappingTools.mapAtomsOfAlignedStructures(molList.get(i), m, map);
								rmsds.add(new DoubleCell(GeometryTools.getAllAtomRMSD(molList.get(i), m, map, true)));
							} else {
								map = AtomMappingTools.mapAtomsOfAlignedStructures(m, molList.get(i), map);
								rmsds.add(new DoubleCell(GeometryTools.getAllAtomRMSD(m, molList.get(i), map, true)));
							}
						} else {
							KabschAlignment sa = new KabschAlignment(m, molList.get(i));
							sa.align();
							rmsds.add(new DoubleCell(sa.getRMSD()));
						}
					}
					molList.add(m);
					double[] rmsdsD = new double[rmsds.size()];
					for (int i = 0; i < rmsds.size(); i++) {
						rmsdsD[i] = rmsds.get(i).getDoubleValue();
					}

					return DistanceVectorDataCellFactory.createCell(rmsdsD, rmsdsD.length);
				} catch (Exception ex) {
					LOGGER.error("Error while calculating RMSD", ex);
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

		RMSDCalculatorSettings s = new RMSDCalculatorSettings();
		s.loadSettings(settings);
		if ((s.targetColumn() == null) || (s.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}

	}
}
