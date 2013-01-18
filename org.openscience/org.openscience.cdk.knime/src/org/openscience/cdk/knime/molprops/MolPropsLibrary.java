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
package org.openscience.cdk.knime.molprops;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.dict.Dictionary;
import org.openscience.cdk.dict.DictionaryDatabase;
import org.openscience.cdk.dict.Entry;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.CDKNodePlugin;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleArrayResultType;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResultType;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.result.IntegerResultType;

/**
 * Utility class that generates various molecular properties using CDK.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public final class MolPropsLibrary {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(MolPropsLibrary.class);

	/** The package name where all CDK molecular descriptor classes reside. */
	public static final String CDK_DESCRIPTOR_PACKAGE = "org.openscience.cdk.qsar.descriptors.molecular";

	/**
	 * Hashes the class name of the IMolecularDescriptor to the actual object that performs the calculation.
	 */
	private static final LinkedHashMap<String, IMolecularDescriptor> DESCRIPTOR_HASH = new LinkedHashMap<String, IMolecularDescriptor>();

	/**
	 * Hashes the class name of the IMolecularDescriptor to a column spec oject suitable to be put into a DataTableSpec.
	 */
	private static final LinkedHashMap<String, DataColumnSpec> DESCRIPTOR_COLSPEC_HASH = new LinkedHashMap<String, DataColumnSpec>();

	/**
	 * Figure out what are the possible properties that this class can calculate.
	 */
	static {
		// file in the cdk-qsar.jar that contains all available descriptors
		String descriptorFileName = "qsar-descriptors.set";
		URL descriptorURL = MolPropsLibrary.class.getClassLoader().getResource(descriptorFileName);
		final List<String> classNames = new ArrayList<String>();
		if (descriptorURL != null) {
			try {
				BufferedReader inStream = new BufferedReader(new InputStreamReader(descriptorURL.openStream()));
				String line;
				while ((line = inStream.readLine()) != null) {
					if (line.startsWith(CDK_DESCRIPTOR_PACKAGE)) {
						classNames.add(line);
					}
				}
			} catch (Exception e) {
				NodeLogger.getLogger(CDKNodePlugin.class).warn("Unable to load descriptors", e);
			}
		} else {
			LOGGER.warn("Unable to load CDK descriptor classes from file " + descriptorFileName);
		}
		final DescriptorEngine engine;
		List<?> nativeDescs;
		Dictionary dict = null;
		try {
			if (!classNames.isEmpty()) {
				engine = new DescriptorEngine(classNames);
			} else {
				engine = new DescriptorEngine(DescriptorEngine.MOLECULAR);
			}
			nativeDescs = engine.getDescriptorInstances();
			dict = new DictionaryDatabase().getDictionary("descriptor-algorithms");
		} catch (Throwable e) {
			LOGGER.warn("Unable to instantiate CDK descriptor engine", e);
			nativeDescs = Collections.emptyList();
		}
		for (Iterator<?> it = nativeDescs.iterator(); it.hasNext();) {
			IDescriptor d = (IDescriptor) it.next();
			String className = d.getSpecification().getImplementationTitle();
			IDescriptorResult resultClass;
			String humanReadable = className;
			try {
				IMolecularDescriptor descriptor;
				if (d instanceof IMolecularDescriptor) {
					descriptor = ((IMolecularDescriptor) d);
					resultClass = descriptor.getDescriptorResultType();
					String id = descriptor.getSpecification().getSpecificationReference();
					int hashIndex = id.indexOf('#');
					id = hashIndex >= 0 && hashIndex < id.length() ? id.substring(hashIndex + 1) : id;
					id = id.toLowerCase();
					if ((dict != null) && dict.hasEntry(id)) {
						// tmp hack for the 3D descriptors
						Entry e = dict.getEntry(id);
						humanReadable = e.getLabel();
						if (id.equals("cpsa")) humanReadable += " (3D)";
					} else {
						LOGGER.warn("No entry: " + id);
					}
				} else {
					LOGGER.debug("Unknown descriptor type for: " + className);
					continue;
				}
				DataColumnSpec colSpec;
				if (resultClass instanceof IntegerResult || resultClass instanceof IntegerResultType) {
					colSpec = new DataColumnSpecCreator(humanReadable, IntCell.TYPE).createSpec();
				} else if (resultClass instanceof DoubleResult || resultClass instanceof DoubleResultType) {
					colSpec = new DataColumnSpecCreator(humanReadable, DoubleCell.TYPE).createSpec();
				} else if (resultClass instanceof DoubleArrayResultType) {
					colSpec = new DataColumnSpecCreator(humanReadable, ListCell.getCollectionType(DoubleCell.TYPE))
							.createSpec();
				} else if (resultClass instanceof IntegerArrayResultType) {
					colSpec = new DataColumnSpecCreator(humanReadable, ListCell.getCollectionType(IntCell.TYPE))
							.createSpec();
				} else {
					LOGGER.debug("Descriptor result (\"" + resultClass + "\") unkown, " + "skipping descriptor "
							+ humanReadable);
					continue;
				}
				DESCRIPTOR_COLSPEC_HASH.put(className, colSpec);
				DESCRIPTOR_HASH.put(className, descriptor);
			} catch (Throwable e) {
				LOGGER.debug("(" + e.getClass().getSimpleName() + ") Failed to load descriptor " + className, e);
			}
		}
	}

	/**
	 * Get a representative {@link DataColumnSpec} object for a given descriptor.
	 * 
	 * @param descriptorClassName The internal class name for the descriptor.
	 * @return The spec to be used.
	 */
	public static DataColumnSpec getColumnSpec(final String descriptorClassName) {

		if (descriptorClassName == null) {
			throw new NullPointerException("Argument must not be null.");
		} else if (descriptorClassName.equals("molecularformula")) {
			DataColumnSpec mfSpec = new DataColumnSpecCreator("Molecular Formula", StringCell.TYPE).createSpec();
			return mfSpec;
		} else if (descriptorClassName.equals("heavyatoms")) {
			DataColumnSpec haSpec = new DataColumnSpecCreator("Heavy Atoms Count", IntCell.TYPE).createSpec();
			return haSpec;
		} else if (descriptorClassName.equals("molarmass")) {
			DataColumnSpec mmSpec = new DataColumnSpecCreator("Molar Mass", DoubleCell.TYPE).createSpec();
			return mmSpec;
		} else if (descriptorClassName.equals("spthreechar")) {
			DataColumnSpec spSpec = new DataColumnSpecCreator("SP3 Character", DoubleCell.TYPE).createSpec();
			return spSpec;
		}
		
		return DESCRIPTOR_COLSPEC_HASH.get(descriptorClassName);
	}

	/**
	 * Get property for molecule.
	 * 
	 * @param rowKey Name of row - used error message.
	 * @param mol The input molecule
	 * @param descriptorClassName class name of the descriptor
	 * @return a <code>DataCell</code> with the property or a missing cell if something goes wrong
	 */
	public static DataCell getProperty(final String rowKey, final IAtomContainer mol, final String descriptorClassName, Object[] params) {

		if (descriptorClassName == null) {
			throw new NullPointerException("Description must not be null.");
		}
		if (!DESCRIPTOR_HASH.containsKey(descriptorClassName)) {
			LOGGER.warn("No such CDK descriptor: \"" + descriptorClassName + "\", assigning missing cell.");
			return DataType.getMissingCell();
		}
		IMolecularDescriptor engine = DESCRIPTOR_HASH.get(descriptorClassName);
		boolean isInt = engine.getDescriptorResultType() instanceof IntegerResult;
		boolean isDouble = engine.getDescriptorResultType() instanceof DoubleResult;
		boolean isDoubleType = engine.getDescriptorResultType() instanceof DoubleResultType;
		boolean isDoubleArray = engine.getDescriptorResultType() instanceof DoubleArrayResult;
		boolean isDoubleArrayType = engine.getDescriptorResultType() instanceof DoubleArrayResultType;
		try {
			DescriptorValue val;
			synchronized (engine) {
				if (params.length > 0) engine.setParameters(params);
				val = engine.calculate(mol);
			}
			IDescriptorResult d = val.getValue();
			if (isInt) {
				int i;
				if (d instanceof IntegerResult) {
					i = ((IntegerResult) d).intValue();
					return new IntCell(i);
				} else if (d instanceof DoubleResult) {
					double dbl = ((DoubleResult) d).doubleValue();
					i = (int) Math.round(dbl);
					LOGGER.debug("qsar descriptor \"" + descriptorClassName + "\" for \"" + mol
							+ "\" didn't return integer " + "but " + dbl + ", rounding to " + i);
					return new IntCell(i);
				} else {
					LOGGER.debug("Unable to handle descriptor result \"" + d.getClass().getSimpleName()
							+ "\", returning missing cell");
				}
			} else if (isDouble || isDoubleType) {
				double i;
				if (d instanceof IntegerResult) {
					i = ((IntegerResult) d).intValue();
					return new DoubleCell(i);
				} else if (d instanceof DoubleResult) {
					i = ((DoubleResult) d).doubleValue();
					return new DoubleCell(i);
				} else {
					LOGGER.debug("Unable to handle descriptor result \"" + d.getClass().getSimpleName()
							+ "\", returning missing cell");
				}
			} else if (isDoubleArray || isDoubleArrayType) {
				DoubleArrayResult dr = (DoubleArrayResult) d;
				Collection<DoubleCell> resultCol = new ArrayList<DoubleCell>();
				for (int i = 0; i < dr.length(); i++) {
					double res = dr.get(i);
					resultCol.add(new DoubleCell(res));
				}
				DataCell cell = CollectionCellFactory.createListCell(resultCol);
				return cell;
			}
		} catch (ClassCastException cce) {
			LOGGER.warn("Unable to get property \"" + descriptorClassName + "\" for molecule in row \"" + rowKey
					+ "\": " + "Return value is not a double; assigning missing", cce);
		} catch (Exception e) {
			LOGGER.warn("Exception (" + e.getClass().getSimpleName() + ") while computing descriptor \""
					+ descriptorClassName + "\" for molecule in row \"" + rowKey + "\": " + e.getMessage(), e);
		}
		return DataType.getMissingCell();
	}

	private MolPropsLibrary() {

		// nothing to do
	}
}
