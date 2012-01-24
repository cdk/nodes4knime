/* Created on 20.01.2012 10:58:41 by Stephan Beisken
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2012 Stephan Beisken <beisken@ebi.ac.uk>
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
 * ------------------------------------------------------------------- * 
 */
package org.openscience.cdk.knime.opsin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.io.tablecreator.prop.SmilesTypeHelper;
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
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.CMLReader;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

/**
 * Cell factory utilizing the OPSIN web service to fetch a PNG, InChI, SMILES,
 * CML, or CDK representation of an IUPAC conform name.
 * 
 * @author Stephan Beisken
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
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		StringBuilder cmlBuilder = null;
		BufferedReader reader = null;
		URL url;

		int i = 0;
		SmilesTypeHelper smilesTypeHelper = SmilesTypeHelper.INSTANCE;
		for (String suffix : urlSuffix) {
			try {
				url = new URL("http://opsin.ch.cam.ac.uk/opsin/" + iupacKey + "." + suffix);
				if (suffix.equals("inchi")) {
					reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
					newCells[i] = new StringCell(reader.readLine());
					reader.close();
				} else if (suffix.equals("smi")) {
					reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
					newCells[i] = smilesTypeHelper.newInstance(reader.readLine());
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
						url = new URL("http://opsin.ch.cam.ac.uk/opsin/" + iupacKey + ".cml");
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
					IAtomContainer container = ChemFileManipulator.getAllAtomContainers(chemFile).get(0);
					AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(container);
					sdg.setMolecule((IMolecule) container);
					sdg.generateCoordinates();
					container = sdg.getMolecule();
					newCells[i] = new CDKCell(container);
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
