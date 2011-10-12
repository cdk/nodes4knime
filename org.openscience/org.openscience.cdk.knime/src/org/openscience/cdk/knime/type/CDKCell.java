/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
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
import org.openscience.cdk.Molecule;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.cml.CMLCoreModule;
import org.openscience.cdk.io.cml.CMLStack;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.libio.cml.ICMLCustomizer;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.xmlcml.cml.element.CMLAtomType;

/**
 * Smiles {@link DataCell} holding a string as internal representation.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class CDKCell extends BlobDataCell implements CDKValue, StringValue {
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
	 * Returns the preferred value class of this cell implementation. This
	 * method is called per reflection to determine which is the preferred
	 * renderer, comparator, etc.
	 * 
	 * @return StringValue.class
	 */
	public static final Class<? extends DataValue> getPreferredValueClass() {
		return CDKValue.class;
	}

	private static final NodeLogger LOGGER = NodeLogger.getLogger(CDKCell.class);

	/**
	 * Name of the data column spec property that indicates if the molecules in
	 * the column have 2D coordinates.
	 */
	public static final String COORD2D_AVAILABLE = "2D coordinates available";

	/**
	 * Name of the data column spec property that indicates if the molecules in
	 * the column have 3D coordinates.
	 */
	public static final String COORD3D_AVAILABLE = "3D coordinates available";

	/** Static instance of the serializer that uses CML as transfer format. */
	private static final DataCellSerializer<CDKCell> SERIALIZER = new CDKSerializer();

	/**
	 * Returns the factory to read/write DataCells of this class from/to a
	 * DataInput/DataOutput. This method is called via reflection.
	 * 
	 * @return A serializer for reading/writing cells of this kind.
	 * @see DataCell
	 */
	public static final DataCellSerializer<CDKCell> getCellSerializer_XXX() {
		// remove "_XXX" from method name to activate
		// note that atom types are not recovered after deserialization
		return SERIALIZER;
	}

	/**
	 * The visual representation for this Smiles cell.
	 */
	private IAtomContainer m_cdkMol;

	/**
	 * Factory method to be used for creation. It will parse the smiles string
	 * and if that is successful it will create a new instance of smiles cell,
	 * otherwise it will return a SmilesType.SMILES_TYP.getMissing() instance.
	 * 
	 * @param smiles the smiles string to parse
	 * @return a new CDKCell if possible, otherwise a missing cell
	 */
	public static final DataCell newInstance(final String smiles) {
		IAtomContainer cdkMol = createMol(smiles);
		if (cdkMol == null) {
			LOGGER.debug("Assigning missing cell for " + smiles);
			return DataType.getMissingCell();
		}
		try {
			CDKHueckelAromaticityDetector.detectAromaticity(cdkMol);
			StructureDiagramGenerator sdg = new StructureDiagramGenerator();
			sdg.setMolecule((IMolecule) cdkMol);
			sdg.generateCoordinates();
			cdkMol = sdg.getMolecule();
			assert (GeometryTools.has2DCoordinates(cdkMol));
		} catch (Exception e) {
			LOGGER.warn("Unable to generate 2D coordinates for" + " molecule: " + smiles, e);
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
			LOGGER.error("Error while creating CML string", ex);
			return "Error while creating CML string";
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated use {@link #getAtomContainer()} instead
	 */
	@Deprecated
	@Override
	public IMolecule getMolecule() {
		return new Molecule(m_cdkMol);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAtomContainer getAtomContainer() {
		return m_cdkMol;
	}

	/**
	 * Used to store this cell into the config.
	 */
	private static final String KEY = "smiles";

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
	 * Returns a new CDKCell with the String value retrieved from the node
	 * settings.
	 * 
	 * @param config to read the smiles's String value from
	 * @return a new CDKCell or <code>null</code>
	 * @throws InvalidSettingsException if the key is not available in the node
	 *             settings
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
		// assert (m_cdkMol == null);
		// m_cdkMol = createMol(m_smiles);
	}

	/**
	 * Factory method to create a {@link IMolecule} from a Smiles string.
	 * 
	 * @param smiles Smiles representation to convert
	 * @return a CDK Molecule for the Smiles string or <code>null</code> if the
	 *         Smiles can't be parsed or is <code>null</code>
	 */
	public static final IMolecule createMol(final String smiles) {
		if (smiles == null) {
			return null;
		}
		LOGGER.debug("Creating molecule for \"" + smiles + "\"... ");
		SmilesParser parser = new SmilesParser(NoNotificationChemObjectBuilder.getInstance());
		IMolecule cdkMol = null;
		try {
			long time = System.currentTimeMillis();
			cdkMol = parser.parseSmiles(smiles);
			long delay = System.currentTimeMillis() - time;
			LOGGER.debug("Successful (" + delay + "ms).");
		} catch (Exception e) {
			LOGGER.debug("Failed", e);
			LOGGER.warn("Molecule could not be generated from \"" + smiles + "\": " + e.getMessage(), e);
		}

		// JOEMol class not found in lib, cdk smiles parser used
		// JOEMol mol = new JOEMol(IOTypeHolder.instance().getIOType("SMILES"),
		// IOTypeHolder.instance().getIOType("SDF"));
		// try {
		// if (!JOESmilesParser.smiToMol(mol, smiles, "Name:" + smiles)) {
		// LOGGER.warn("Molecule could not be generated from \""
		// + smiles + "\".");
		// }
		// } catch (Exception e) {
		// LOGGER.warn("Molecule could not be generated from \""
		// + smiles + "\"", e);
		// }
		// Molecule cdkMol = Convertor.convert(mol);
		return cdkMol;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		return this.getStringValue().equals(((CDKCell) dc).getStringValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return m_cdkMol.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getStringValue();
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
