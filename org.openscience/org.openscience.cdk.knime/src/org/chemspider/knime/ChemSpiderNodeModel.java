/*
 * 
 * Copyright (C) 2007-2013 Egon Willighagen <egon.willighagen@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby
 * granted, provided that the above copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
 * AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */
package org.chemspider.knime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.chem.types.MolCell;
import org.knime.chem.types.MolCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of ChemSpider. Download Structures from ChemSpider.com.
 * 
 * @TODO should also work with InChI
 * 
 * @author Egon Willighagen
 */
public class ChemSpiderNodeModel extends NodeModel {

	private String inchikeyColumnName = null;

	// the logger instance
	private static final NodeLogger logger = NodeLogger.getLogger(ChemSpiderNodeModel.class);

	private static final Pattern pattern = Pattern.compile("Chemical-Structure.(\\d*).html");

	/**
	 * Constructor for the node model.
	 */
	protected ChemSpiderNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		ColumnRearranger rearranger = rearrangeColumns(inData[0].getDataTableSpec());

		BufferedDataTable someName = exec.createColumnRearrangeTable(inData[0], rearranger, exec);

		return new BufferedDataTable[] { someName };

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// do nothing
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (inSpecs[0].findColumnIndex(inchikeyColumnName) == -1) {
			throw new InvalidSettingsException("Cannot find the InChIKey column!");
		}

		ColumnRearranger rearranger = rearrangeColumns(inSpecs[0]);

		return new DataTableSpec[] { rearranger.createSpec() };
	}

	private ColumnRearranger rearrangeColumns(final DataTableSpec inSpecs) {

		ColumnRearranger bla = new ColumnRearranger(inSpecs);
		DataColumnSpecCreator specCreator = new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(inSpecs,
				"Mol File"), MolCell.TYPE);

		final int colIndex = inSpecs.findColumnIndex(inchikeyColumnName);

		bla.append(new SingleCellFactory(specCreator.createSpec()) {

			@Override
			public DataCell getCell(final DataRow row) {

				String inchiKey = ((StringValue) row.getCell(colIndex)).getStringValue();
				try {
					URL url = new URL("http://www.chemspider.com/InChIKey/" + inchiKey);
					BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection()
							.getInputStream()));
					String line = reader.readLine();
					String csid = "";
					while (line != null) {
						Matcher matcher = pattern.matcher(line);
						if (matcher.find()) {
							csid = matcher.group(1);
							logger.debug("Found CSID: " + csid);
						}
						line = reader.readLine();
					}

					url = new URL("http://www.chemspider.com/mol/" + csid);
					reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));

					StringBuilder molFile = new StringBuilder();
					line = reader.readLine();
					while (line != null) {
						molFile.append(line).append('\n');
						line = reader.readLine();
					}
					return MolCellFactory.create(molFile.toString());
				} catch (Exception exception) {
					logger.error(exception.getMessage(), exception);
					return DataType.getMissingCell();
				}
			}
		});
		return bla;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		settings.addString("inchikeyColumnName", inchikeyColumnName);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		inchikeyColumnName = settings.getString("inchikeyColumnName");

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		settings.getString("inchikeyColumnName");

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// TODO load internal data.
		// Everything handed to output ports is loaded automatically (data
		// returned by the execute method, models loaded in loadModelContent,
		// and user settings set through loadSettingsFrom - is all taken care
		// of). Load here only the other internals that need to be restored
		// (e.g. data used by the views).

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// TODO save internal models.
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).

	}
}
