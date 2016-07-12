package org.openscience.cdk.knime.nodes.depiction.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.openscience.cdk.knime.core.CDKSettings;

/**
 * Abstract node settings. Initially based off the {@link CDKSettings} to handle
 * the structure column.
 * <br></br>
 * Provides some default implementation on top to provide auto handling of saving, validating and loading
 * <br></br>
 * Implementers should implement the {@link #addSettings()} method. 
 * 
 * @since KNIME 3.1
 * @since CDK 1.5.600 ?
 * @author Samuel Webb, Lhasa Limited
 *
 */
public abstract class NodeSettingCollection implements CDKSettings
{
	public static final String CONFIG_STRUCTURE_COLUMN = "cfgStructureColumn";

	protected String m_molColumn = null;

	protected Map<String, SettingsModel> settingMap;

	protected abstract void addSettings();

	public NodeSettingCollection()
	{
		super();
		settingMap = new HashMap<String, SettingsModel>();
		addSettings();
	}

	/**
	 * Returns the name of the column that holds the molecules.
	 * 
	 * @return a column name
	 */
	public String targetColumn()
	{
		return m_molColumn;
	}

	/**
	 * Sets the name of the column that holds the molecules.
	 * 
	 * @param columnName
	 *            a column name
	 */
	public void targetColumn(final String columnName)
	{
		m_molColumn = columnName;
	}

	/**
	 * Loads the settings from the given node settings object.
	 * 
	 * @param settings
	 *            node settings
	 * @throws InvalidSettingsException
	 *             if some settings are missing
	 */
	public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException
	{

		for (SettingsModel setting : settingMap.values())
		{
			try
			{
				setting.loadSettingsFrom(settings);
			} catch (Exception e)
			{
				throw new InvalidSettingsException(e);
			}
		}

		targetColumn(((SettingsModelColumnName) getSetting(CONFIG_STRUCTURE_COLUMN, null)).getColumnName());
	}

	/**
	 * Saves the settings to the given node settings object.
	 * 
	 * @param settings
	 *            node settings
	 */
	public void saveSettings(final NodeSettingsWO settings)
	{
		for (SettingsModel setting : settingMap.values())
		{
			try
			{
				setting.saveSettingsTo(settings);
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new UncheckedIOException("", new IOException(e));
			}
		}
	}

	public void loadSettingsForDialog(final NodeSettingsRO settings)
	{
		try
		{
			loadSettings(settings);
		} catch (InvalidSettingsException e)
		{
			e.printStackTrace();
		}

		targetColumn(((SettingsModelColumnName) getSetting(CONFIG_STRUCTURE_COLUMN, null)).getColumnName());
	}

	/**
	 * Iterator for the stored {@link SettingsModel} objects
	 * 
	 * @return
	 */
	public Iterator<SettingsModel> getSettingIterator()
	{
		return settingMap.values().iterator();
	}

	/**
	 * Get the desired setting cast to the given type.
	 * 
	 * @param key
	 *            The key (see the static strings)
	 * @param type
	 *            The class of the {@link SettingsModel}
	 * @return
	 */
	public <T> T getSetting(String key, Class<T> type)
	{
		return (T) settingMap.get(key);
	}

	public void validateSettings(NodeSettingsRO settings)
	{
		for (SettingsModel s : settingMap.values())
		{

			try
			{
				s.validateSettings(settings);
			} catch (InvalidSettingsException e)
			{
				e.printStackTrace();
			}

		}

		targetColumn(getSetting(CONFIG_STRUCTURE_COLUMN, SettingsModelColumnName.class).getColumnName());
	}

	public void loadValidatedSettingsFrom(NodeSettingsRO settings)
	{
		for (SettingsModel s : settingMap.values())
		{
			try
			{
				s.loadSettingsFrom(settings);
			} catch (InvalidSettingsException e)
			{
				e.printStackTrace();
			}
		}

		targetColumn(getSetting(CONFIG_STRUCTURE_COLUMN, SettingsModelColumnName.class).getColumnName());
	}

	public void saveSettingsTo(NodeSettingsWO settings)
	{
		targetColumn(getSetting(CONFIG_STRUCTURE_COLUMN, SettingsModelColumnName.class).getColumnName());

		for (SettingsModel model : settingMap.values())
		{
			model.saveSettingsTo(settings);
		}
	}

}
