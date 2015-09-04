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
package org.openscience.cdk.knime.type;

import javax.swing.Icon;

import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * DataValue for a Molecule.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface CDKValue extends DataValue {

	/**
	 * Meta information to this value type.
	 *
	 * @see DataValue#UTILITY
	 */
	public static final UtilityFactory UTILITY = new CDKUtilityFactory();

	/**
	 * Returns the Smiles string of the molecule.
	 *
	 * @return a String value
	 */
	String getSmilesValue();

	/**
	 * Get the CDK atom container.
	 *
	 * @return the corresponding atom container
	 */
	IAtomContainer getAtomContainer();

	/** Implementations of the meta information of this value class. */
	public static class CDKUtilityFactory extends ExtensibleUtilityFactory {

		/** Singleton icon to be used to display this cell type. */
		private static final Icon ICON = loadIcon(CDKValue.class, "/cdk.png");

		private static final DataValueComparator COMPARATOR = new DataValueComparator() {

			@Override
			protected int compareDataValues(final DataValue v1, final DataValue v2) {

				final int BEFORE = -1;
				final int EQUAL = 0;
				final int AFTER = 1;

				return v1.hashCode() < v2.hashCode() ? BEFORE : (v1.hashCode() == v2.hashCode() ? EQUAL : AFTER);
			}
		};

		/** Only subclasses are allowed to instantiate this class. */
		protected CDKUtilityFactory() {
			super(CDKValue.class);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Icon getIcon() {
			return ICON;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected DataValueComparator getComparator() {
			return COMPARATOR;
		}

		@Override
		public String getName() {
			return "CDK Molecule";
		}
	}
}
