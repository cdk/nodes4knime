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
package org.openscience.cdk.knime.type;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Element;

import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.BlobDataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.cml.CMLCoreModule;
import org.openscience.cdk.io.cml.CMLStack;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.libio.cml.ICMLCustomizer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.xmlcml.cml.element.CMLAtomType;

/**
 * Smiles {@link DataCell} holding a string as internal representation.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class CDKCell extends BlobDataCell implements CDKValue, SmilesValue, SdfValue, StringValue {

	static {
		CMLWriter writer = new CMLWriter();
		writer.registerCustomizer(new ICMLCustomizer() {

			@Override
			public void customize(final IAtom atom, final Object nodeToAdd) throws Exception {

				if (atom.getAtomTypeName() != null) {
					if (nodeToAdd instanceof Element) {
						Element element = (Element) nodeToAdd;
						CMLAtomType atomType = new CMLAtomType();
						atomType.setConvention("bioclipse:atomType");
						atomType.appendChild(atom.getAtomTypeName());
						element.appendChild(atomType);
					}
				}
			}

			// don't customize the rest
			@Override
			public void customize(final IBond bond, final Object nodeToAdd) throws Exception {

			}

			@Override
			public void customize(final IAtomContainer molecule, final Object nodeToAdd) throws Exception {

			}
		});
	}

	/**
	 * Convenience access member for <code>DataType.getType(CDKCell)</code>.
	 * 
	 * @see DataType#getType(Class)
	 */
	public static final DataType TYPE = DataType.getType(CDKCell.class);

	/**
	 * Returns the preferred value class of this cell implementation. This method is called per reflection to determine
	 * which is the preferred renderer, comparator, etc.
	 * 
	 * @return StringValue.class
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {

		return CDKValue.class;
	}

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKCell.class);

	/**
	 * Name of the data column spec property that indicates if the molecules in the column have 2D coordinates.
	 */
	public static final String COORD2D_AVAILABLE = "2D coordinates available";

	/**
	 * Name of the data column spec property that indicates if the molecules in the column have 3D coordinates.
	 */
	public static final String COORD3D_AVAILABLE = "3D coordinates available";

	/**
	 * Static instance of the serializer that uses CML as transfer format.
	 */
	private static final DataCellSerializer<CDKCell> SERIALIZER = new CDKSerializer();

	/**
	 * Returns the factory to read/write DataCells of this class from/to a DataInput/DataOutput. This method is called
	 * via reflection.
	 * 
	 * @return A serializer for reading/writing cells of this kind.
	 * @see DataCell
	 */
	public static final DataCellSerializer<CDKCell> getCellSerializer() {

		return SERIALIZER;
	}

	/**
	 * Used to store this cell into the config.
	 */
	private static final String KEY = "smiles";

	/**
	 * The visual representation for this Smiles cell.
	 */
	private final IAtomContainer m_cdkMol;

	/**
	 * Factory method to be used for creation. It will parse the smiles string and if that is successful it will create
	 * a new instance of smiles cell, otherwise it will return a SmilesType.SMILES_TYP.getMissing() instance.
	 * 
	 * @param smiles the smiles string to parse
	 * @return a new CDKCell if possible, otherwise a missing cell
	 */
	public static final DataCell newInstance(final String smiles) {

		IAtomContainer cdkMol = createMol(smiles);

		if (cdkMol == null) {
			return DataType.getMissingCell();
		}

		cdkMol.setProperty(KEY, smiles);

		try {
			CDKNodeUtils.getStandardMolecule(cdkMol);
			cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, true);
		} catch (CDKException exception) {
			return DataType.getMissingCell();
		}

		return new CDKCell(cdkMol);
	}

	/**
	 * Creates new CDK cell.
	 * 
	 * @param atomContainer the CDK atom container
	 */
	public CDKCell(final IAtomContainer atomContainer) {

		m_cdkMol = atomContainer;
		CDKNodeUtils.calculateSmiles(atomContainer);
	}

	/**
	 * Returns the internal string value.
	 * 
	 * @return The string value.
	 */
	@Override
	public String getStringValue() {

		StringWriter stringWriter = new StringWriter(8192);
		CMLWriter writer = new CMLWriter(stringWriter);

		try {
			writer.write(m_cdkMol);
			return stringWriter.toString();
		} catch (CDKException ex) {
			return "Error while creating CML string";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {

		return (String) m_cdkMol.getProperty(KEY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSdfValue() {

		IAtomContainer cdkMol = m_cdkMol;
		SDFWriter sdfWriter = null;
		StringWriter stringWriter = null;

		try {
			if (!GeometryTools.has2DCoordinates(m_cdkMol)) {
				if (!GeometryTools.has3DCoordinates(m_cdkMol)) {

					StructureDiagramGenerator sdg = new StructureDiagramGenerator();
					sdg.setMolecule(m_cdkMol);
					sdg.generateCoordinates();
					cdkMol = sdg.getMolecule();
				}
			}

			stringWriter = new StringWriter();
			sdfWriter = new SDFWriter(stringWriter);

			sdfWriter.write(cdkMol);

		} catch (CDKException exception) {
			LOGGER.error("Error while cwriting sdf", exception);
		} finally {
			try {
				sdfWriter.close();
				stringWriter.close();
			} catch (IOException exception) {
			}
		}
		return stringWriter.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAtomContainer getAtomContainer() {

		return m_cdkMol;
	}

	/**
	 * Stores only the smiles' String into the node settings.
	 * 
	 * @param config to write the String value into
	 * 
	 * @see #load
	 */
	public void save(final NodeSettings config) {

		config.addString(KEY, getStringValue());
	}

	/**
	 * Returns a new CDKCell with the String value retrieved from the node settings.
	 * 
	 * @param config to read the smiles's String value from
	 * @return a new CDKCell or <code>null</code>
	 * @throws InvalidSettingsException if the key is not available in the node settings
	 * @throws NullPointerException if the smiles string is <code>null</code>
	 * @see #save
	 */
	public static DataCell load(final NodeSettings config) throws InvalidSettingsException {

		String smiles = config.getString(KEY);
		if (smiles != null) {
			return CDKCell.newInstance(smiles);
		}
		return DataType.getMissingCell();
	}

	private void writeObject(final ObjectOutputStream out) throws IOException {

		out.defaultWriteObject();
	}

	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {

		in.defaultReadObject();
	}

	/**
	 * Factory method to create an AtomContainer from a Smiles string.
	 * 
	 * @param smiles Smiles representation to convert
	 * @return a CDK Molecule for the Smiles string or <code>null</code> if the Smiles can't be parsed or is
	 *         <code>null</code>
	 * 
	 */
	private static IAtomContainer createMol(final String smiles) {

		if (smiles == null) {
			return null;
		}

		SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
		IAtomContainer cdkMol = null;
		try {
			cdkMol = parser.parseSmiles(smiles);
		} catch (Exception exception) {
			LOGGER.error("Error while parsing smiles", exception);
		}

		return cdkMol;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {

		return this.getSmilesValue().equals(((CDKCell) dc).getSmilesValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {

		return m_cdkMol.getProperty(KEY).hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		return getSdfValue();
	}

	/** Factory for (de-)serializing a DoubleCell. */
	private static class CDKSerializer implements DataCellSerializer<CDKCell> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(final CDKCell cell, final DataCellDataOutput out) throws IOException {

			byte[] data = cell.getStringValue().getBytes();
			out.writeInt(data.length);
			out.write(data);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CDKCell deserialize(final DataCellDataInput input) throws IOException {

			int size = input.readInt();
			byte[] cml = new byte[size];
			input.readFully(cml);

			try {
				CMLReader reader = new CMLReader(new ByteArrayInputStream(cml));

				reader.registerConvention("bioclipse:atomType", new CMLCoreModule((IChemFile) null) {

					List<String> atomTypes = new ArrayList<String>();

					@Override
					protected void newAtomData() {

						super.newAtomData();
						atomTypes = new ArrayList<String>();
					}

					@Override
					protected void storeAtomData() {

						super.storeAtomData();

						boolean hasAtomType = false;
						if (atomTypes.size() == atomCounter) {
							hasAtomType = true;
						} else {
							logger.debug("No atom types: " + elid.size(), " != " + atomCounter);
						}
						if (hasAtomType) {
							for (int i = 0; i < atomCounter; i++) {
								currentAtom = currentMolecule.getAtom(i);
								currentAtom.setAtomTypeName(atomTypes.get(i));
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
						} else {
							super.endElement(xpath, uri, name, raw);
						}
					}
				});
				IChemFile chemFile = (ChemFile) reader.read(new ChemFile());
				return new CDKCell(ChemFileManipulator.getAllAtomContainers(chemFile).get(0));
			} catch (CDKException ex) {
				LOGGER.error("Error while deserializing CDK cell via CML", ex);
				throw new IOException(ex.getMessage());
			}
		}
	}
}
