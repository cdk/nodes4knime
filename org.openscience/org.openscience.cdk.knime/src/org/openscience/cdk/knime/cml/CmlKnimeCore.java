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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.config.AtomTypeFactory;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomType;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Stereo;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.io.cml.CMLCoreModule;
import org.openscience.cdk.io.cml.CMLStack;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
import org.openscience.cdk.tools.manipulator.BondManipulator;

/**
 * CML reader core module facilitating the CML CDK molecule conversion process by reading and parsing the CDK atom types
 * stored in the CML. Instead of calling the CDKAtomTypeMatcher, the CDK atom types are provided, hence avoiding the
 * expensive method call.
 * 
 * @author Stephan Beisken
 */
public class CmlKnimeCore extends CMLCoreModule {

	public static final String CONVENTION = "knime:cml";

	private static final String CDK_ATOM_TYPE = "org/openscience/cdk/dict/data/cdk-atom-types.owl";

	private AtomTypeFactory factory;
	private List<String> atomTypes;
	private IAtomType atomType;

	public CmlKnimeCore() {

		super((IChemFile) null);
		atomTypes = new ArrayList<String>();
		factory = AtomTypeFactory.getInstance(CDK_ATOM_TYPE, SilentChemObjectBuilder.getInstance());
	}

	@Override
	protected void newAtomData() {

		super.newAtomData();
		atomTypes = new ArrayList<String>();
	}

	@Override
	protected void storeAtomData() {

		super.storeAtomData();

		if (atomTypes.size() == atomCounter) {
			for (int i = 0; i < atomCounter; i++) {
				currentAtom = currentMolecule.getAtom(i);
				atomType = getAtomType(atomTypes.get(i));
				if (atomType != null)
					AtomTypeManipulator.configureUnsetProperties(currentAtom, atomType);
			}
		}

		if (atomParities.size() == atomCounter) {
			for (int i = 0; i < atomCounter; i++) {
				if (atomParities.get(i) != null)
					currentMolecule.getAtom(i).setStereoParity(Integer.parseInt(atomParities.get(i)));
			}
		}
	}

	@Override
	protected void storeBondData() {

		logger.debug("Testing a1,a2,stereo,order = count: " + bondARef1.size(), "," + bondARef2.size(), ","
				+ bondStereo.size(), "," + order.size(), "=" + bondCounter);

		if ((bondARef1.size() == bondCounter) && (bondARef2.size() == bondCounter)) {
			logger.debug("About to add bond info...");

			Iterator<String> orders = order.iterator();
			Iterator<String> ids = bondid.iterator();
			Iterator<String> bar1s = bondARef1.iterator();
			Iterator<String> bar2s = bondARef2.iterator();
			Iterator<String> stereos = bondStereo.iterator();
			Iterator<Boolean> aroms = bondAromaticity.iterator();

			while (bar1s.hasNext()) {
				IAtom a1 = (IAtom) atomEnumeration.get((String) bar1s.next());
				IAtom a2 = (IAtom) atomEnumeration.get((String) bar2s.next());
				currentBond = currentChemFile.getBuilder().newInstance(IBond.class, a1, a2);
				if (ids.hasNext()) {
					currentBond.setID((String) ids.next());
				}

				if (orders.hasNext()) {
					String bondOrder = (String) orders.next();

					if ("S".equals(bondOrder)) {
						currentBond.setOrder(CDKConstants.BONDORDER_SINGLE);
					} else if ("D".equals(bondOrder)) {
						currentBond.setOrder(CDKConstants.BONDORDER_DOUBLE);
					} else if ("T".equals(bondOrder)) {
						currentBond.setOrder(CDKConstants.BONDORDER_TRIPLE);
					} else if ("A".equals(bondOrder)) {
						currentBond.setOrder(CDKConstants.BONDORDER_SINGLE);
						currentBond.setFlag(CDKConstants.ISAROMATIC, true);
						currentBond.setFlag(CDKConstants.SINGLE_OR_DOUBLE, true);
					} else {
						currentBond.setOrder(BondManipulator.createBondOrder(Double.parseDouble(bondOrder)));
					}
				}

				if (stereos.hasNext()) {
					String nextStereo = (String) stereos.next();
					if ("H".equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.DOWN);
					} else if ("W".equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.UP);
					} else if (Stereo.UP_INVERTED.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.UP_INVERTED);
					} else if (Stereo.DOWN_INVERTED.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.DOWN_INVERTED);
					} else if (Stereo.UP_OR_DOWN.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.UP_OR_DOWN);
					} else if (Stereo.UP_OR_DOWN_INVERTED.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.UP_OR_DOWN_INVERTED);
					} else if (Stereo.E_OR_Z.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.E_OR_Z);
					} else if (Stereo.E.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.E);
					} else if (Stereo.Z.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.Z);
					} else if (Stereo.E_Z_BY_COORDINATES.name().equals(nextStereo)) {
						currentBond.setStereo(IBond.Stereo.E_Z_BY_COORDINATES);
					} else if (nextStereo != null) {
						logger.warn("Cannot interpret stereo information: " + nextStereo);
					}
				}

				if (aroms.hasNext()) {
					Object nextArom = aroms.next();
					if (nextArom != null && nextArom == Boolean.TRUE) {
						currentBond.setFlag(CDKConstants.ISAROMATIC, true);
					}
				}

				if (currentBond.getID() != null) {
					Map<String, String> currentBondProperties = bondCustomProperty.get(currentBond.getID());
					if (currentBondProperties != null) {
						Iterator<String> keys = currentBondProperties.keySet().iterator();
						while (keys.hasNext()) {
							String key = keys.next();
							currentBond.setProperty(key, currentBondProperties.get(key));
						}
						bondCustomProperty.remove(currentBond.getID());
					}
				}

				currentMolecule.addBond(currentBond);
			}
		}
	}

	@Override
	public void endElement(final CMLStack xpath, final String uri, final String name, final String raw) {

		if (xpath.endsWith("atom", "atomType")) {
			while ((atomTypes.size() + 1) < atomCounter) {
				atomTypes.add(null);
			}
			atomTypes.add(currentChars);
		} else if (xpath.endsWith("atom", "atomParity")) {
			while ((atomParities.size() + 1) < atomCounter) {
				atomParities.add(null);
			}
			atomParities.add(currentChars);
		} else {
			super.endElement(xpath, uri, name, raw);
		}
	}

	/**
	 * Returns the atom type for a atom type string or null if no such atom type exists.
	 * 
	 * @param identifier the atom type string
	 * @return the atom type
	 */
	private IAtomType getAtomType(String identifier) {

		IAtomType type = null;
		try {
			type = factory.getAtomType(identifier);
			type.setValency((Integer) type.getProperty(CDKConstants.PI_BOND_COUNT) + type.getFormalNeighbourCount());
		} catch (CDKException exception) {
			// fall through
		}
		return type;
	}
}
