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
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.hydrogen;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.knime.base.node.parallel.appender.AppendColumn;
import org.knime.base.node.parallel.appender.ColumnDestination;
import org.knime.base.node.parallel.appender.ExtendedCellFactory;
import org.knime.base.node.parallel.appender.ReplaceColumn;
import org.knime.base.node.parallel.appender.ThreadedColAppenderNodeModel;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.hydrogen.HydrogenAdderSettings.Conversion;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.normalize.SMSDNormalizer;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

/**
 * This is the model for the hydrogen node that performs all computation by
 * using CDK functionality.
 * 
 * @author Thorsten Meinl, University of Konstanz
 * @author Stephan Beisken, EMBL-EBI
 */
public class HydrogenAdderNodeModel extends ThreadedColAppenderNodeModel {
	private static final Map<String, String> NO_PROP_2D;

	private static final NodeLogger LOGGER = NodeLogger.getLogger(HydrogenAdderNodeModel.class);

	static {
		Map<String, String> temp = new TreeMap<String, String>();
		temp.put(CDKCell.COORD2D_AVAILABLE, "false");
		NO_PROP_2D = Collections.unmodifiableMap(temp);
	}

	private final HydrogenAdderSettings m_settings = new HydrogenAdderSettings();

	/**
	 * Creates a new model having one input and one output node.
	 */
	public HydrogenAdderNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		int molCol = inSpecs[0].findColumnIndex(m_settings.molColumnName());
		if (molCol == -1) {
			for (DataColumnSpec dcs : inSpecs[0]) {
				if (dcs.getType().isCompatible(CDKValue.class)) {
					if (molCol >= 0) {
						molCol = -1;
						break;
					} else {
						molCol = inSpecs[0].findColumnIndex(dcs.getName());
					}
				}
			}

			if (molCol != -1) {
				String name = inSpecs[0].getColumnSpec(molCol).getName();
				setWarningMessage("Using '" + name + "' as molecule column");
				m_settings.molColumnName(name);
			}
		}

		if (molCol == -1) {
			throw new InvalidSettingsException("Molecule column '" + m_settings.molColumnName() + "' does not exist");
		}

		DataColumnSpec[] colSpecs;
		if (m_settings.replaceColumn()) {
			colSpecs = new DataColumnSpec[inSpecs[0].getNumColumns()];
			for (int i = 0; i < colSpecs.length; i++) {
				colSpecs[i] = inSpecs[0].getColumnSpec(i);
			}
			DataColumnSpec newcol = createColSpec(inSpecs[0]);
			colSpecs[molCol] = newcol;
		} else {
			colSpecs = new DataColumnSpec[inSpecs[0].getNumColumns() + 1];
			for (int i = 0; i < colSpecs.length - 1; i++) {
				colSpecs[i] = inSpecs[0].getColumnSpec(i);
			}
			String name = m_settings.appendColumnName();
			if (name == null || name.length() == 0) {
				throw new InvalidSettingsException("Invalid name for appended column");
			}
			if (inSpecs[0].containsName(name)) {
				throw new InvalidSettingsException("Duplicate column name: " + name);
			}
			DataColumnSpec dc = createColSpec(inSpecs[0]);
			colSpecs[colSpecs.length - 1] = dc;

		}
		return new DataTableSpec[] { new DataTableSpec(colSpecs) };
	}

	/**
	 * Creates the column spec that is used to represent the new column, either
	 * replaced or appended.
	 * 
	 * @param in The original input spec.
	 * @return The single column spec to use.
	 */
	private DataColumnSpec createColSpec(final DataTableSpec in) {
		if (m_settings.replaceColumn()) {
			DataColumnSpec original = in.getColumnSpec(m_settings.molColumnName());
			DataColumnSpecCreator creator = new DataColumnSpecCreator(original);
			creator.setProperties(original.getProperties().cloneAndOverwrite(NO_PROP_2D));
			creator.setType(CDKCell.TYPE);
			return creator.createSpec();
		}
		DataColumnSpecCreator creator = new DataColumnSpecCreator(m_settings.appendColumnName(), CDKCell.TYPE);
		return creator.createSpec();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		HydrogenAdderSettings s = new HydrogenAdderSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ExtendedCellFactory[] prepareExecute(final DataTable[] data) throws Exception {
		final int molColIndex = data[0].getDataTableSpec().findColumnIndex(m_settings.molColumnName());
		ExtendedCellFactory cf = new ExtendedCellFactory() {
			@Override
			public DataCell[] getCells(final DataRow row) {
				DataCell molCell = row.getCell(molColIndex);
				if (molCell.isMissing()) {
					return new DataCell[] { DataType.getMissingCell() };
				}

				try {
					IAtomContainer oldMol = ((CDKValue) molCell).getAtomContainer();
					IAtomContainer newMol = (IAtomContainer) oldMol.clone();
					CDKHydrogenAdder hyda = CDKHydrogenAdder.getInstance(NoNotificationChemObjectBuilder.getInstance());
					hyda.addImplicitHydrogens(newMol);
					if (m_settings.getConversion().equals(Conversion.ToExplicit)) {
						AtomContainerManipulator.convertImplicitToExplicitHydrogens(newMol);
					} else if (m_settings.getConversion().equals(Conversion.ToImplicit)) {
						newMol = SMSDNormalizer.convertExplicitToImplicitHydrogens(newMol);
					}
					CDKCell newCell = new CDKCell(newMol);
					return new DataCell[] { newCell };
				} catch (Throwable t) {
					LOGGER.warn(t);
					return new DataCell[] { DataType.getMissingCell() };
				}
			}

			@Override
			public ColumnDestination[] getColumnDestinations() {
				if (m_settings.replaceColumn()) {
					return new ColumnDestination[] { new ReplaceColumn(molColIndex) };
				} else {
					return new ColumnDestination[] { new AppendColumn() };
				}
			}

			@Override
			public DataColumnSpec[] getColumnSpecs() {
				return new DataColumnSpec[] { createColSpec(data[0].getDataTableSpec()) };
			}
		};

		return new ExtendedCellFactory[] { cf };
	}
}
