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
package org.openscience.cdk.knime.nodes.descriptors.whim3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.core.CDKNodeModel;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WHIMDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;

/**
 * This is the model implementation of Whim3d. Holistic descriptors described by Todeschini et al. The descriptors are
 * based on a number of atom weightings. There are 5 different possible weightings implemented.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Whim3dNodeModel extends CDKNodeModel {

	/**
	 * Constructor for the node model.
	 */
	protected Whim3dNodeModel() {
		super(1, 1, new Whim3dSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ColumnRearranger createColumnRearranger(final DataTableSpec spec) throws InvalidSettingsException {
		
		final List<Whim3dSchemes> weightingSchemes = new ArrayList<Whim3dSchemes>();
		final DataColumnSpec[] dataColSpec = createSpec();

		for (DataColumnSpec outputColumnSpec : dataColSpec) {

			if (outputColumnSpec.getName().equals(Whim3dSchemes.UNITY_WEIGHTS.getTitle()))
				weightingSchemes.add(Whim3dSchemes.UNITY_WEIGHTS);
			if (outputColumnSpec.getName().equals(Whim3dSchemes.ATOMIC_MASSES.getTitle()))
				weightingSchemes.add(Whim3dSchemes.ATOMIC_MASSES);
			if (outputColumnSpec.getName().equals(Whim3dSchemes.ATOMIC_POLARIZABILITIES.getTitle()))
				weightingSchemes.add(Whim3dSchemes.ATOMIC_POLARIZABILITIES);
			if (outputColumnSpec.getName().equals(Whim3dSchemes.VdW_VOLUMES.getTitle()))
				weightingSchemes.add(Whim3dSchemes.VdW_VOLUMES);
			if (outputColumnSpec.getName().equals(Whim3dSchemes.ATOMIC_ELECTRONEGATIVITIES.getTitle()))
				weightingSchemes.add(Whim3dSchemes.ATOMIC_ELECTRONEGATIVITIES);
		}

		final IMolecularDescriptor whimDescriptor = new WHIMDescriptor();

		AbstractCellFactory cf = new AbstractCellFactory(true, dataColSpec) {

			@Override
			public DataCell[] getCells(final DataRow row) {
				
				DataCell[] whimValueCells = new DataCell[dataColSpec.length];
				
				if (row.getCell(columnIndex).isMissing()
						|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
					Arrays.fill(whimValueCells, DataType.getMissingCell());
					return whimValueCells;
				}
				
				CDKValue cdkCell = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
				IAtomContainer molecule = cdkCell.getAtomContainer();

				if (!ConnectivityChecker.isConnected(molecule)) {
					molecule = ConnectivityChecker.partitionIntoMolecules(molecule).getAtomContainer(0);
				}
				
				return calculateWhimValues(molecule);
			}
			
			private DataCell[] calculateWhimValues(IAtomContainer molecule) {

				DataCell[] whimValueCells = new DataCell[dataColSpec.length];

				int cellIndex = 0;
				for (Whim3dSchemes weightingScheme : weightingSchemes) {
					whimValueCells[cellIndex] = calculateValueForScheme(weightingScheme, molecule);
					cellIndex++;
				}

				return whimValueCells;
			}

			private DataCell calculateValueForScheme(Whim3dSchemes scheme, IAtomContainer molecule) {

				try {
					Object[] whimParameter = new String[] { scheme.getParameterName() };
					whimDescriptor.setParameters(whimParameter);

					// try catch because WHIM works for certain elements only
					DescriptorValue whimValue = whimDescriptor.calculate(molecule);
					DoubleArrayResult whimResultArray = (DoubleArrayResult) whimValue.getValue();

					return getDataCell(whimResultArray);

				} catch (Exception exception) {
					return DataType.getMissingCell();
				}
			}

			private DataCell getDataCell(DoubleArrayResult whimResultArray) {

				Collection<DoubleCell> resultCol = new ArrayList<DoubleCell>();
				for (int i = 0; i < whimResultArray.length(); i++) {
					double res = whimResultArray.get(i);
					resultCol.add(new DoubleCell(res));
				}
				DataCell cell = CollectionCellFactory.createListCell(resultCol);

				return cell;
			}
		};
		
		ColumnRearranger arranger = new ColumnRearranger(spec);
		arranger.ensureColumnIsConverted(CDKTypeConverter.createConverter(spec, columnIndex), columnIndex);
		arranger.append(cf);
		return arranger;
	}

	private DataColumnSpec[] createSpec() {

		List<DataColumnSpec> dataColumnSpecs = new ArrayList<DataColumnSpec>();

		if (settings(Whim3dSettings.class).isSchemeUnitWeights())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.UNITY_WEIGHTS.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings(Whim3dSettings.class).isSchemeAtomicMasses())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_MASSES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings(Whim3dSettings.class).isSchemeAtomicPolariz())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_POLARIZABILITIES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings(Whim3dSettings.class).isSchemeVdWVolumes())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.VdW_VOLUMES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings(Whim3dSettings.class).isSchemeAtomicElectronneg())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_ELECTRONEGATIVITIES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		
		return dataColumnSpecs.toArray(new DataColumnSpec[] {});
	}

	/**
	 * Creates a single column specification.
	 */
	private void createColumnSpec(List<DataColumnSpec> dataColumnSpecs, String colName, DataType cellType) {

		DataColumnSpec colSpec = new DataColumnSpecCreator(colName, cellType).createSpec();
		dataColumnSpecs.add(colSpec);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		Whim3dSettings tmpSettings = new Whim3dSettings();
		tmpSettings.loadSettings(settings);

		if ((tmpSettings.targetColumn() == null) || (tmpSettings.targetColumn().length() == 0)) {
			throw new InvalidSettingsException("No compatible molecule column chosen");
		}
	}
}
