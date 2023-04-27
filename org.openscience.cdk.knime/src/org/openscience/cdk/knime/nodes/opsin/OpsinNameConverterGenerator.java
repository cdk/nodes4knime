/*
 * Copyright (c) 2016, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.knime.nodes.opsin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.knime.chem.types.SmilesCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.type.CDKCell3;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

/**
 * Cell factory utilizing the OPSIN web service to fetch a PNG, InChI, SMILES, CML, or CDK representation of an IUPAC
 * conform name.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class OpsinNameConverterGenerator implements CellFactory {

	private final DataColumnSpec[] dataColumnSpec;
	private final List<String> urlSuffix;
	private final int iupacColIndex;

	/**
	 * Constructs the cell factory.
	 * 
	 * @param iupacColIndex a string column index
	 * @param urlSuffix a list of OPSIN webservice url suffices
	 * @param dataColumnSpec a data column spec list complementing the url suffix list
	 */
	public OpsinNameConverterGenerator(int iupacColIndex, List<String> urlSuffix, DataColumnSpec[] dataColumnSpec) {

		this.dataColumnSpec = dataColumnSpec;
		this.iupacColIndex = iupacColIndex;
		this.urlSuffix = urlSuffix;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell[] getCells(DataRow row) {

		DataCell iupacCell = row.getCell(iupacColIndex);
		DataCell[] newCells = new DataCell[dataColumnSpec.length];
		if (iupacCell.isMissing()) {
			Arrays.fill(newCells, DataType.getMissingCell());
			return newCells;
		}
		if (!(iupacCell instanceof StringValue)) {
			throw new IllegalArgumentException("No String cell at " + iupacColIndex + ": "
					+ iupacCell.getClass().getName());
		}

		String iupacKey = ((StringValue) row.getCell(iupacColIndex)).getStringValue();
		StringBuilder cmlBuilder = null;
		BufferedReader reader = null;
		URL url;

		int i = 0;
		for (String suffix : urlSuffix) {
			try {
				url = new URL("https://opsin.ch.cam.ac.uk/opsin/" + iupacKey + "." + suffix);
				if (suffix.equals("inchi")) {
					reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
					newCells[i] = new StringCell(reader.readLine());
					reader.close();
				} else if (suffix.equals("smi")) {
					reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
					newCells[i] = SmilesCellFactory.create(reader.readLine());
					reader.close();
				} else if (suffix.equals("cml")) {
					if (cmlBuilder == null) {
						reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
						String line = "";
						cmlBuilder = new StringBuilder();
						while ((line = reader.readLine()) != null) {
							cmlBuilder.append(line + "\n");
						}
						reader.close();
					}
					newCells[i] = XMLCellFactory.create(cmlBuilder.toString());
				} else if (suffix.equals("png")) {
					BufferedInputStream bis = new BufferedInputStream(url.openConnection().getInputStream());
					PNGImageContent content = new PNGImageContent(bis);
					newCells[i] = content.toImageCell();
					bis.close();
				} else if (suffix.equals("cdk")) {
					if (cmlBuilder == null) {
						url = new URL("https://opsin.ch.cam.ac.uk/opsin/" + iupacKey + ".cml");
						reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
						String line = "";
						cmlBuilder = new StringBuilder();
						while ((line = reader.readLine()) != null) {
							cmlBuilder.append(line + "\n");
						}
						reader.close();
					}
					InputStream bais = new ByteArrayInputStream(cmlBuilder.toString().getBytes());
					CMLReader cmlReader = new CMLReader(bais);
					IChemFile chemFile = new ChemFile();
					chemFile = (IChemFile) cmlReader.read(chemFile);
					cmlReader.close();
					IAtomContainer container = ChemFileManipulator.getAllAtomContainers(chemFile).get(0);
					// OPSIN WS return has explicit Hs
					container = SMSDNormalizer.convertExplicitToImplicitHydrogens(container);
					CDKNodeUtils.getFullMolecule(container);
					container = CDKNodeUtils.calculateCoordinates(container, true, false);
					newCells[i] = CDKCell3.createCDKCell(container);
					bais.close();
				}
				i++;
			} catch (Exception exception) {
				newCells[i] = DataType.getMissingCell();
				i++;
			}
		}
		return newCells;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataColumnSpec[] getColumnSpecs() {

		return dataColumnSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {
		exec.setProgress(curRowNr / (double) rowCount, "Retrieved conversions for row " + curRowNr + " (\"" + lastKey
				+ "\")");
	}
}
