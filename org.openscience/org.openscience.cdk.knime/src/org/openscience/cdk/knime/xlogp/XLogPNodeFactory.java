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
package org.openscience.cdk.knime.xlogp;

import org.openscience.cdk.knime.molprops.AbstractSinglePropertyNodeFactory;

/**
 * @author wiswedel, University of Konstanz
 */
public class XLogPNodeFactory extends AbstractSinglePropertyNodeFactory {

	private static final String XLOGP_DESCRIPTOR_CLASSNAME = "org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor";

	/** Instantiates super class with appropriate class description. */
	public XLogPNodeFactory() {

		super(XLOGP_DESCRIPTOR_CLASSNAME);
	}
}
