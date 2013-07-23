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
 * History 09.11.2012 (meinl): created
 */
package org.openscience.cdk.knime.type;

import java.io.IOException;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterCell;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Adapter cell implementation for CDK.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class CDKAdapterCell extends AdapterCell implements CDKValue, SmilesValue, SdfValue, StringValue {

	/**
	 * The raw type of this adapter cell with only the implemented value classes. The type of the cell may change if
	 * additional adapters are added.
	 */
	public static final DataType RAW_TYPE = DataType.getType(CDKAdapterCell.class);

	private static final AdapterCellSerializer<CDKAdapterCell> SERIALIZER = new AdapterCellSerializer<CDKAdapterCell>() {

		@Override
		public CDKAdapterCell deserialize(final DataCellDataInput input) throws IOException {
			return new CDKAdapterCell(input);
		}
	};

	public static DataCellSerializer<CDKAdapterCell> getCellSerializer() {
		return SERIALIZER;
	}

	private CDKAdapterCell(final DataCellDataInput input) throws IOException {
		super(input);
	}

	/**
	 * Creates a new CDK adapter cell based on a CDK cell.
	 * 
	 * @param cell the CDK cell that should be added to the adapter
	 */
	public CDKAdapterCell(final DataCell cell) {
		super(cell);
	}

	/**
	 * See {@link DataCell} description for details.
	 * 
	 * @return CDKValue.class
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {
		return CDKValue.class;
	}

	/**
	 * Creates a new CDK adapter cell based on a CDK cell and an existing adapter cell. All cells in the given adapter
	 * are copied into this new cell.
	 * 
	 * @param copy an existing adapter whose values are copied
	 * @param cell the CDK cell that should be added to the adapter
	 */
	public CDKAdapterCell(final AdapterValue copy, final DataCell cell) {
		super(cell, copy);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSdfValue() {
		return ((SdfValue) lookupFromAdapterMap(SdfValue.class)).getSdfValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getStringValue() {
		return ((StringValue) lookupFromAdapterMap(StringValue.class)).getStringValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {
		return ((SmilesValue) lookupFromAdapterMap(SmilesValue.class)).getSmilesValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAtomContainer getAtomContainer() {
		return ((CDKValue) lookupFromAdapterMap(CDKValue.class)).getAtomContainer();
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public String toString() {
		return ((CDKValue) lookupFromAdapterMap(CDKValue.class)).toString();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {

		int hashRef = ((AdapterValue) dc).getAdapter(CDKValue.class).hashCode();
		return this.hashCode() == hashRef;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return ((CDKValue) lookupFromAdapterMap(CDKValue.class)).hashCode();
	}
}