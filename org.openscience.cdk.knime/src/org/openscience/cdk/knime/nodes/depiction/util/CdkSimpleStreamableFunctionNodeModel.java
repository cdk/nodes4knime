package org.openscience.cdk.knime.nodes.depiction.util;

import org.knime.chem.types.InchiValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmartsCell;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCellTypeConverter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RWAdapterValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionNodeModel;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.core.CDKAdapterNodeModel;
import org.openscience.cdk.knime.type.CDKTypeConverter;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * An extension of the {@link SimpleStreamableFunctionNodeModel} to add the {@link CDKAdapterNodeModel} functionality
 * with a streaming API implementation. 
 * <br></br>
 * This class assumes the use of a {@link NodeSettingCollection} which handles saving, loading and validating the {@link SettingsModel}
 * objects. 
 * <br></br>
 * Implementers of this class must provide an implementation of {@link #createCellFactory(DataTableSpec)} which will be appended
 * to the input table if not overriding the {@link #createColumnRearranger(DataTableSpec)}. To replace a cell then the 
 * {@link #createColumnRearranger(DataTableSpec)} must also be overridden. 
 * 
 * @TODO move this class should it be adopted outside of the depiction node
 *  
 * @author Samuel Webb, Lhasa Limited
 * @since KNIME v3.1, CDK plugin v1.5.500
 *
 */
public abstract class CdkSimpleStreamableFunctionNodeModel extends SimpleStreamableFunctionNodeModel
{

	protected int columnIndex;
	
	/** The settings for the node. Cast to the specific class to get access to the {@link SettingsModel}*/
	protected NodeSettingCollection localSettings;
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException
	{
		autoConfigure(inSpecs);
		return new DataTableSpec[] { createColumnRearranger(inSpecs[0]).createSpec() };
	}
	
	/**
	 * Auto-configures the input column from the data table specification.
	 * 
	 * @param inSpecs
	 *            the input data table specification
	 * @throws InvalidSettingsException
	 *             if the input specification is not compatible
	 */
	protected void autoConfigure(final DataTableSpec[] inSpecs) throws InvalidSettingsException
	{

		if (localSettings.targetColumn() == null)
		{
			String name = null;
			for (DataColumnSpec s : inSpecs[0])
			{
				if (s.getType().isAdaptable(CDKValue.class))
				{ // prefer CDK column, use other as fallback
					name = s.getName();
				} else if ((name == null) && s.getType().isAdaptableToAny(CDKNodeUtils.ACCEPTED_VALUE_CLASSES))
				{
					name = s.getName();
				}

				// hack to circumvent empty adapter value list map
				if ((name == null) && isAdaptableToAny(s))
				{
					name = s.getName();
				}
			}
			if (name != null)
			{
				localSettings.targetColumn(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else
			{
				throw new InvalidSettingsException("No CDK compatible column in input table");
			}
		}

		columnIndex = inSpecs[0].findColumnIndex(localSettings.targetColumn());
	}

	/**
	 * Checks the data type of the column spec for CDK compatibility.
	 * 
	 * @param s
	 *            the data column spec
	 * @return if compatible
	 */
	private boolean isAdaptableToAny(DataColumnSpec s)
	{

		for (Class<? extends DataValue> cl : CDKNodeUtils.ACCEPTED_VALUE_CLASSES)
		{
			if (cl == s.getType().getPreferredValueClass())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
	{
		localSettings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException
	{
		localSettings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException
	{
		localSettings.validateSettings(settings);
	}
	
	/**
	 * Checks if the selected molecule type needs conversion to a CDK type.
	 * 
	 * @param spec
	 *            the original input spec
	 * @return if conversion needed
	 */
	protected boolean needsConversion(final DataTableSpec spec)
	{

		if (columnIndex >= spec.getNumColumns())
		{
			return false;
		}
		DataType type = spec.getColumnSpec(columnIndex).getType();

		if (type.isCompatible(AdapterValue.class))
		{
			if (type.isAdaptable(CDKValue.class) || type.isCompatible(CDKValue.class))
			{
				return false;
			} else if (type.isCompatible(RWAdapterValue.class) && type.isCompatible(StringValue.class)
					&& type.isCompatible(SmilesValue.class) && type.isCompatible(SdfValue.class)
					&& type.isCompatible(InchiValue.class))
			{
				return true;
			} else if (type.isAdaptable(SdfValue.class))
			{
				return true;
			} else if (type.isAdaptable(SmilesValue.class))
			{
				return true;
			} else if (type.isAdaptable(InchiValue.class))
			{
				return true;
			} else if (type.isAdaptable(StringValue.class))
			{
				return true;
			}
		} else
		{
			if (type.isCompatible(CDKValue.class))
			{
				return true;
			} else if (type.isCompatible(SdfValue.class))
			{
				return true;
			} else if (type.isCompatible(SmilesValue.class))
			{
				return true;
			} else if (type.isCompatible(InchiValue.class))
			{
				return true;
			} else if (type.isCompatible(StringValue.class) && !type.equals(SmartsCell.TYPE))
			{
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Create a cell factory that will be used to append to the input table
	 * @param spec
	 * @return
	 */
	protected abstract AbstractCellFactory createCellFactory(final DataTableSpec spec);
	
	@Override
	public ColumnRearranger createColumnRearranger(final DataTableSpec spec)
	{
		final ColumnRearranger rearranger = new ColumnRearranger(spec);

		if (needsConversion(spec))
		{
			final DataCellTypeConverter converter = CDKTypeConverter.createConverter(spec, columnIndex);
			rearranger.ensureColumnIsConverted(converter, columnIndex);
		}

		rearranger.append(createCellFactory(spec));

		return rearranger;
	}


}
