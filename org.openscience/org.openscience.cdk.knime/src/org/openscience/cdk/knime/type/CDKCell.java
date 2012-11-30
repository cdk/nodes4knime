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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.knime.core.node.NodeLogger;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.io.CMLWriter;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.knime.CDKNodeUtils;
import org.openscience.cdk.knime.cml.CmlKnimeCore;
import org.openscience.cdk.knime.cml.CmlKnimeCustomizer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

/**
 * Smiles {@link DataCell} holding a string as internal representation.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public final class CDKCell extends BlobDataCell implements CDKValue, SmilesValue, SdfValue, StringValue {

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
	 * The visual representation for this CDK cell.
	 */
	private final String compressedCml;
	/**
	 * The hash code.
	 */
	private final int hash;

	/**
	 * Factory method to be used for creation. It will parse the SDF string and if that is successful it will create a
	 * new instance of the CDKCell, otherwise it will return a missing cell instance.
	 * 
	 * @param sdf the sdf string to parse
	 * @return a new CDKCell if possible, otherwise a missing cell
	 */
	public static final DataCell newInstance(final String sdf) {

		DataCell resultCell = DataType.getMissingCell();

		if (sdf != null && !sdf.isEmpty()) {

			IAtomContainer cdkMol = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);
			IteratingSDFReader reader = new IteratingSDFReader(new StringReader(sdf),
					SilentChemObjectBuilder.getInstance());

			try {
				while (reader.hasNext()) {
					cdkMol.add(reader.next());
				}
				if (cdkMol != null) {
					CDKNodeUtils.getStandardMolecule(cdkMol);
					cdkMol = CDKNodeUtils.calculateCoordinates(cdkMol, false, false);
				}
				resultCell = new CDKCell(cdkMol);
			} catch (Exception e) {
				// do nothing
			}
		}

		return resultCell;
	}

	/**
	 * Creates new CDK cell.
	 * 
	 * @param atomContainer the CDK atom container
	 */
	public CDKCell(IAtomContainer atomContainer) {

		compressedCml = getCompressedCml(atomContainer);

		CDKNodeUtils.calculateHash(atomContainer);
		hash = (Integer) atomContainer.getProperty(CDKConstants.MAPPED);

		atomContainer = null;
	}

	/**
	 * Creates new CDK cell.
	 * 
	 * @param compressedCml the CML string
	 * @param hash the CDK hash
	 */
	public CDKCell(final String compressedCml, final int hash) {

		this.compressedCml = compressedCml;
		this.hash = hash;
	}

	/**
	 * Returns the internal string value.
	 * 
	 * @return The string value.
	 */
	@Override
	public String getStringValue() {

		BufferedReader br;
		GZIPInputStream gis;
		String decompressedCml = "";

		try {
			gis = new GZIPInputStream(new ByteArrayInputStream(compressedCml.getBytes("ISO-8859-1")));
			br = new BufferedReader(new InputStreamReader(gis));

			StringBuilder sb = new StringBuilder();

			String line;
			while ((line = br.readLine()) != null) {
				// clean output from CDK artifacts
				if (line.contains(CmlKnimeCore.CONVENTION) || line.contains("cdk:aromaticAtom")
						|| line.contains("cdk:aromaticBond"))
					continue;
				sb.append(line);
				sb.append("\n");
			}

			gis.close();
			br.close();
			decompressedCml = sb.toString();
		} catch (IOException ex) {
			// do nothing;
		}

		return decompressedCml;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSmilesValue() {

		return CDKNodeUtils.calculateSmiles(getMol(), false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSdfValue() {

		IAtomContainer cdkMolSdf = getMol();

		if (cdkMolSdf.getAtomCount() == 0)
			return "";

		SDFWriter sdfWriter = null;
		StringWriter stringWriter = null;

		try {
			cdkMolSdf = CDKNodeUtils.calculateCoordinates(cdkMolSdf, false, false);

			stringWriter = new StringWriter();
			sdfWriter = new SDFWriter(stringWriter);

			sdfWriter.write(cdkMolSdf);

		} catch (CDKException exception) {
			LOGGER.error("Error while cwriting sdf", exception);
		} finally {
			try {
				sdfWriter.close();
				stringWriter.close();
			} catch (IOException exception) {
				// do nothing
			}
		}
		return stringWriter.toString();
	}

	/**
	 * Returns the compressed CML string of the molecule.
	 * 
	 * @return the compressed CML string
	 */
	public String getCmlValue() {

		return compressedCml;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAtomContainer getAtomContainer() {

		return getMol();
	}

	private IAtomContainer getMol() {

		IAtomContainer mol = SilentChemObjectBuilder.getInstance().newInstance(IAtomContainer.class);

		if (compressedCml == null || compressedCml.length() == 0) {
			return mol;
		}

		try {
			GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressedCml.getBytes("ISO-8859-1")));
			CMLReader reader = new CMLReader(gis);
			reader.registerConvention(CmlKnimeCore.CONVENTION, new CmlKnimeCore());

			IChemFile chemFile = (ChemFile) reader.read(new ChemFile());
			mol = ChemFileManipulator.getAllAtomContainers(chemFile).get(0);
			mol.setProperty(CDKConstants.MAPPED, hash);

			gis = null;
			reader = null;
			chemFile = null;
		} catch (Exception exception) {
			// do nothing
		} finally {

		}

		return mol;
	}

	private String getCompressedCml(IAtomContainer cdkMol) {

		String outStr = "";
		StringWriter stringWriter = new StringWriter(8192);
		CMLWriter writer = new CMLWriter(stringWriter);
		writer.registerCustomizer(new CmlKnimeCustomizer());

		try {
			writer.write(cdkMol);
			String value = stringWriter.toString();

			stringWriter.close();
			writer.close();

			stringWriter = null;
			writer = null;

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(value.getBytes());

			out.close();
			gzip.close();

			outStr = out.toString("ISO-8859-1");

			value = null;
			gzip = null;
			out = null;
		} catch (Exception ex) {
			// do nothing;
		}

		return outStr;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean equalsDataCell(final DataCell dc) {

		int hashRef = ((CDKCell) dc).hashCode();
		return this.hashCode() == hashRef;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {

		return hash;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {

		return getStringValue();
	}

	/**
	 * Factory for (de-)serializing a DoubleCell.
	 */
	private static class CDKSerializer implements DataCellSerializer<CDKCell> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void serialize(final CDKCell cell, final DataCellDataOutput out) throws IOException {

			out.writeUTF(cell.getCmlValue());
			out.writeInt(cell.hashCode());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public CDKCell deserialize(final DataCellDataInput input) throws IOException {

			return new CDKCell(input.readUTF(), input.readInt());
		}
	}
}
