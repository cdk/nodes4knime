/*
 * ------------------------------------------------------------------------
 * 
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License, Version 3, as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, see
 * <http://www.gnu.org/licenses>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * 
 * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs. Hence, KNIME and ECLIPSE are both independent
 * programs and are not derived from each other. Should, however, the interpretation of the GNU GPL Version 3
 * ("License") under any applicable laws result in KNIME and ECLIPSE being a combined program, KNIME GMBH herewith
 * grants you the additional permission to use and propagate KNIME together with ECLIPSE with only the license terms in
 * place for ECLIPSE applying to ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the license terms of
 * ECLIPSE themselves allow for the respective use and propagation of ECLIPSE together with KNIME.
 * 
 * Additional permission relating to nodes for KNIME that extend the Node Extension (and in particular that are based on
 * subclasses of NodeModel, NodeDialog, and NodeView) and that only interoperate with KNIME through standard APIs
 * ("Nodes"): Nodes are deemed to be separate and independent programs and to not be covered works. Notwithstanding
 * anything to the contrary in the License, the License does not apply to Nodes, you are not required to license Nodes
 * under the License, and you are granted a license to prepare and propagate Nodes, in each case even if such Nodes are
 * propagated with or for interoperation with KNIME. The owner of a Node may freely choose the license terms applicable
 * to such Node, including when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 * 
 * History 01.11.2012 (meinl): created
 */
package org.openscience.cdk.knime.type;

import java.io.StringReader;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RWAdapterValue;
import org.knime.core.data.StringValue;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

/**
 * Converter for CDK that converts Smiles or SDF cells into an adapter cell that contains CDK cells.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public abstract class CDKTypeConverter extends DataCellTypeConverter {

	private final DataType m_outputType;

	private CDKTypeConverter(final DataType outputType) {
		super(true); // parallel processing enabled
		m_outputType = outputType;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataType getOutputType() {
		return m_outputType;
	}

	/**
	 * Creates a new converter for a specific column in a table. The output type and the specific converter that is used
	 * is determined automatically from the input type.
	 * 
	 * @param tableSpec the input table's spec
	 * @param columnIndex the index of the column that should be converted.
	 * @return a new converter
	 */
	@SuppressWarnings("unchecked")
	public static CDKTypeConverter createConverter(final DataTableSpec tableSpec, final int columnIndex) {
		DataType type = tableSpec.getColumnSpec(columnIndex).getType();

		if (type.isCompatible(AdapterValue.class)) {

			if (type.isCompatible(CDKValue.class)) {

				// We have already an Adapter cell that is compatible with CDK Value - we return it
				return new CDKTypeConverter(type) {

					/**
					 * {@inheritDoc} Just returns the existing CDK Value within a new CDK Adapter Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {
						return source;
					}
				};
			} else if (type.isCompatible(RWAdapterValue.class) && type.isCompatible(StringValue.class)
					&& type.isCompatible(SmilesValue.class) && type.isCompatible(SdfValue.class)) {
				// we have a writable adapter cell that already represents all value
				// interfaces of CDK (except CDK) thus we can just add the CDKCell
				return new CDKTypeConverter(type.createNewWithAdapter(CDKValue.class)) {

					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						String sdf = ((RWAdapterValue) source).getAdapter(SdfValue.class).getSdfValue();
						MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdf));
						IAtomContainer cdkMol = reader.read(SilentChemObjectBuilder.getInstance().newInstance(
								IAtomContainer.class));
						CDKNodeUtils.getStandardMolecule(cdkMol);
						cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
						return ((RWAdapterValue) source).cloneAndAddAdapter(new CDKCell(cdkMol), CDKValue.class);
					}
				};
			} else if (type.isAdaptable(SdfValue.class)) {
				return new CDKTypeConverter(DataType.getType(CDKAdapterCell.class, null, type.getValueClasses())) {

					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						String sdf = ((AdapterValue) source).getAdapter(SdfValue.class).getSdfValue();
						MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdf));
						IAtomContainer cdkMol = reader.read(SilentChemObjectBuilder.getInstance().newInstance(
								IAtomContainer.class));
						CDKNodeUtils.getStandardMolecule(cdkMol);
						cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
						return CDKCell.createCDKCell(source, cdkMol);
					}
				};
			} else if (type.isAdaptable(SmilesValue.class)) {
				return new CDKTypeConverter(DataType.getType(CDKAdapterCell.class, null, type.getValueClasses())) {

					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						String smiles = ((AdapterValue) source).getAdapter(SmilesValue.class).getSmilesValue();
						SmilesParser reader = new SmilesParser(SilentChemObjectBuilder.getInstance());
						IAtomContainer cdkMol = reader.parseSmiles(smiles);
						CDKNodeUtils.getStandardMolecule(cdkMol);
						cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
						return CDKCell.createCDKCell(source, cdkMol);
					}
				};
			}
		} else {
			if (type.isCompatible(CDKValue.class)) {

				// We have already an CDK Value - we just create from it an CDK Adapter Cell
				return new CDKTypeConverter(CDKAdapterCell.RAW_TYPE) {

					/**
					 * {@inheritDoc} Just returns the existing CDK Value within a new CDK Adapter Cell.
					 */
					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						return new CDKAdapterCell(source);
					}
				};
			}
			// no adapter cell => create a new CDKAdapterCell
			else if (type.isCompatible(SdfValue.class)) {
				return new CDKTypeConverter(CDKAdapterCell.RAW_TYPE) {

					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						String sdf = ((SdfValue) source).getSdfValue();
						MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdf));
						IAtomContainer cdkMol = reader.read(SilentChemObjectBuilder.getInstance().newInstance(
								IAtomContainer.class));
						CDKNodeUtils.getStandardMolecule(cdkMol);
						cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
						return CDKCell.createCDKCell(cdkMol);
					}
				};
			} else if (type.isCompatible(SmilesValue.class)) {
				return new CDKTypeConverter(CDKAdapterCell.RAW_TYPE) {

					@Override
					public DataCell convert(final DataCell source) throws Exception {

						if (source == null || source.isMissing()) {
							return DataType.getMissingCell();
						}

						String smiles = ((SmilesValue) source).getSmilesValue();
						SmilesParser reader = new SmilesParser(SilentChemObjectBuilder.getInstance());
						IAtomContainer cdkMol = reader.parseSmiles(smiles);
						CDKNodeUtils.getStandardMolecule(cdkMol);
						cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
						return CDKCell.createCDKCell(cdkMol);
					}
				};
			}
		}

		throw new IllegalArgumentException("No converter for type " + type + " exists");
	}
}