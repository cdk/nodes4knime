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
package org.openscience.cdk.knime.cml;

import nu.xom.Element;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.libio.cml.ICMLCustomizer;
import org.xmlcml.cml.element.CMLAtomParity;
import org.xmlcml.cml.element.CMLAtomType;
import org.xmlcml.cml.element.CMLBondStereo;

/**
 * CML writer customizer to write out CDK atom type and bond stereo information.
 * 
 * @author Stephan Beisken
 */
public class CmlKnimeCustomizer implements ICMLCustomizer {

	@Override
	public void customize(final IAtom atom, final Object nodeToAdd) throws Exception {

		if (atom.getAtomTypeName() != null) {
			if (nodeToAdd instanceof Element) {
				Element element = (Element) nodeToAdd;
				
				CMLAtomType atomType = new CMLAtomType();
				atomType.setConvention(CmlKnimeCore.CONVENTION);
				atomType.appendChild(atom.getAtomTypeName());
				element.appendChild(atomType);

				if (atom.getStereoParity() != null) {
					CMLAtomParity atomParity = new CMLAtomParity();
					atomParity.setConvention(CmlKnimeCore.CONVENTION);
					atomParity.appendChild(atom.getStereoParity().toString());
					element.appendChild(atomParity);
				}
			}
		}
	}

	@Override
	public void customize(final IBond bond, final Object nodeToAdd) throws Exception {

		if (bond.getStereo() != null && nodeToAdd instanceof Element) {
			switch (bond.getStereo()) {
			case NONE: // do nothing
				break;
			case UP: // already taken care off by the CMLConverter
				break;
			case DOWN: // already taken care off by the CMLConverter
				break;
			default:
				writeCDKStereo(bond, nodeToAdd);
				break;
			}
		}
	}

	private void writeCDKStereo(IBond bond, Object nodeToAdd) {

		Element element = (Element) nodeToAdd;
		CMLBondStereo bondStereo = new CMLBondStereo();
		bondStereo.setConvention(CmlKnimeCore.CONVENTION);
		bondStereo.appendChild(bond.getStereo().name());
		element.appendChild(bondStereo);
	}

	@Override
	public void customize(final IAtomContainer molecule, final Object nodeToAdd) throws Exception {
		// do nothing
	}
}
