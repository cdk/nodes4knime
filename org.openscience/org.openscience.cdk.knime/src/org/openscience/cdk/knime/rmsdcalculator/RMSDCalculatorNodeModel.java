package org.openscience.cdk.knime.rmsdcalculator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.distmatrix.type.DistanceVectorDataCell;
import org.knime.distmatrix.type.DistanceVectorDataCellFactory;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.AtomMappingTools;
import org.openscience.cdk.knime.rmsdcalculator.RMSDCalculatorSettings.AlignmentTypes;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.normalize.SMSDNormalizer;

/**
 * This is the model implementation of RMSDCalculator.
 * 
 * 
 * @author Luis F. de Figueiredo
 */
public class RMSDCalculatorNodeModel extends NodeModel {

	private final RMSDCalculatorSettings m_settings = new RMSDCalculatorSettings();
	// the logger instance
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(RMSDCalculatorNodeModel.class);

	/**
	 * Constructor for the node model.
	 */
	protected RMSDCalculatorNodeModel() {

		// TODO one incoming port and one outgoing port is assumed
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		ColumnRearranger cr = createColumnRearranger(inData[0]
				.getDataTableSpec());

		return new BufferedDataTable[] { exec.createColumnRearrangeTable(
				inData[0], cr, exec) };

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// TODO Code executed on reset.
		// Models build during execute are cleared here.
		// Also data handled in load/saveInternals will be erased here.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
			throws InvalidSettingsException {

		if (m_settings.molColumnName() == null) {
            String name = null;
            for (DataColumnSpec s : inSpecs[0]) {
                if (s.getType().isCompatible(CDKValue.class)) {
                    name = s.getName();
                }
            }
            if (name != null) {
                m_settings.molColumnName(name);
                setWarningMessage("Auto configuration: Using column \"" + name
                        + "\"");
            } else {
                throw new InvalidSettingsException(
                        "No CDK compatible column in input table");
            }
        }
		// creates a new column with the correct specifications
		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);

		return new DataTableSpec[] { arranger.createSpec() };
	}

	private ColumnRearranger createColumnRearranger(final DataTableSpec inSpecs)
			throws InvalidSettingsException {
		// get column name and check if it is defined
		String molcolname = m_settings.molColumnName();
		if (molcolname == null || !inSpecs.containsName(molcolname)) {
			throw new InvalidSettingsException("No such column: " + molcolname);
		}

		DataColumnSpec cs = inSpecs.getColumnSpec(molcolname);
		// check the datatype of the column
		if (!cs.getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("No CDK column: " + molcolname);
		}
		String newColName = DataTableSpec.getUniqueColumnName(inSpecs, "RMSD");
		DataColumnSpecCreator c = new DataColumnSpecCreator(newColName,
				DistanceVectorDataCell.TYPE);
		DataColumnSpec appendSpec = c.createSpec();

		final int molColIndex = inSpecs.findColumnIndex(molcolname);

		final List<IAtomContainer> molList = new ArrayList<IAtomContainer>();

		SingleCellFactory cf = new SingleCellFactory(appendSpec) {

			@Override
			public DataCell getCell(final DataRow row) {
				if (row.getCell(molColIndex).isMissing()) {
					return DataType.getMissingCell();
				}
				CDKValue mol = (CDKValue) row.getCell(molColIndex);
				try {
					List<DoubleCell> rmsds = new ArrayList<DoubleCell>();
					IAtomContainer con = mol.getAtomContainer();
					SMSDNormalizer.aromatizeMolecule(con);

					if (!GeometryTools.has3DCoordinates(con)) {
						System.out.print(con.getAtomCount());
						return DataType.getMissingCell();
					}

					for (int i = 0; i < molList.size(); i++) {
						if (m_settings.alignmentType().equals(
								AlignmentTypes.Isomorphic)) {
							Map<Integer, Integer> map = new HashMap<Integer, Integer>();
							if (molList.get(i).getAtomCount() > con
									.getAtomCount()) {
								map = AtomMappingTools
										.mapAtomsOfAlignedStructures(
												molList.get(i), con, map);
								rmsds.add(new DoubleCell(GeometryTools
										.getAllAtomRMSD(molList.get(i), con,
												map, true)));
							} else {
								map = AtomMappingTools
										.mapAtomsOfAlignedStructures(con,
												molList.get(i), map);
								rmsds.add(new DoubleCell(GeometryTools
										.getAllAtomRMSD(con, molList.get(i),
												map, true)));
							}

						} else {
							KabschAlignment sa = new KabschAlignment(con,
									molList.get(i));
							sa.align();
							rmsds.add(new DoubleCell(sa.getRMSD()));
						}
					}
					molList.add(con);
					double[] rmsdsD = new double[rmsds.size()];
					for (int i = 0; i < rmsds.size(); i++) {
						rmsdsD[i] = rmsds.get(i).getDoubleValue();
					}

					return DistanceVectorDataCellFactory.createCell(rmsdsD,
							rmsdsD.length);
				} catch (Exception ex) {
					LOGGER.error("Error while calculating RMSD", ex);
					return DataType.getMissingCell();
				}
			}
		};

		ColumnRearranger arranger = new ColumnRearranger(inSpecs);
		arranger.append(cf);
		return arranger;
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
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO load (valid) settings from the config object.
		// It can be safely assumed that the settings are valided by the
		// method below.

		m_settings.loadSettings(settings);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings)
			throws InvalidSettingsException {

		// TODO check if the settings could be applied to our model
		// e.g. if the count is in a certain range (which is ensured by the
		// SettingsModel).
		// Do not actually set any values of any member variables.

		RMSDCalculatorSettings s = new RMSDCalculatorSettings();
		s.loadSettings(settings);
		if ((s.molColumnName() == null) || (s.molColumnName().length() == 0)) {
			throw new InvalidSettingsException("No molecule column chosen");
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
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
	protected void saveInternals(final File internDir,
			final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// TODO save internal models.
		// Everything written to output ports is saved automatically (data
		// returned by the execute method, models saved in the saveModelContent,
		// and user settings saved through saveSettingsTo - is all taken care
		// of). Save here only the other internals that need to be preserved
		// (e.g. data used by the views).

	}

}
