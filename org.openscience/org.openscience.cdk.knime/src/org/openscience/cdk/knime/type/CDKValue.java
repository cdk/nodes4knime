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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;
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
	public static class CDKUtilityFactory extends UtilityFactory {

		/** Singleton icon to be used to display this cell type. */
		private static final Icon ICON = loadIcon(CDKValue.class, "/cdk.png");

		private static final DataValueComparator COMPARATOR = new DataValueComparator() {

			@Override
			protected int compareDataValues(final DataValue v1, final DataValue v2) {

				int hash1 = ((CDKValue) v1).hashCode();
				int hash2 = ((CDKValue) v2).hashCode();
				return hash1 - hash2;
			}
		};

		/** Only subclasses are allowed to instantiate this class. */
		protected CDKUtilityFactory() {

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

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected DataValueRendererFamily getRendererFamily(final DataColumnSpec spec) {

			return new DefaultDataValueRendererFamily(new CDKValueRenderer(),
					new MultiLineStringValueRenderer("String"));
		}
	}
}
