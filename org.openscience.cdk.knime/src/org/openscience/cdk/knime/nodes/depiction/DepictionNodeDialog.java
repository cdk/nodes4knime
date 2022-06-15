package org.openscience.cdk.knime.nodes.depiction;

import java.util.Arrays;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColorChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Depiction" Node. Depict CDK structures into
 * images
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Sameul Webb, Lhasa Limited
 */
public class DepictionNodeDialog extends DefaultNodeSettingsPane
{

	DepictionSettings settings = new DepictionSettings();

	SettingsModelBoolean atomHighlightSetting;
	SettingsModelColumnName atomHighlights;

	SettingsModelBoolean bondHighlightSetting;
	SettingsModelColumnName bondHighlights;

	/**
	 * New pane for configuring the Depiction node.
	 */
	protected DepictionNodeDialog()
	{
		addDialogComponent(new DialogComponentColumnNameSelection(
				settings.getSetting(DepictionSettings.CONFIG_STRUCTURE_COLUMN, SettingsModelColumnName.class),
				"Structure column", 0, DataValue.class));

		createNewGroup("Image settings");
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentStringSelection(
				settings.getSetting(DepictionSettings.CONFIG_IMAGE_FORMAT, SettingsModelString.class), "Format",
				Arrays.asList(new String[] { "PNG", "SVG" })));

		addDialogComponent(new DialogComponentNumber(
				settings.getSetting(DepictionSettings.CONFIG_IMAGE_WIDTH, SettingsModelInteger.class), "Width", 10));

		addDialogComponent(new DialogComponentNumber(
				settings.getSetting(DepictionSettings.CONFIG_IMAGE_HEIGHT, SettingsModelInteger.class), "Height", 10));

		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_FILL_TO_FIT, SettingsModelBoolean.class),
				"Fill to fit?"));

		/////////
		// General settings
		createGeneralSettingsOptions();

		///////////////////
		///// Higlighting
		createHighlightingOptions();

	}

	private void createGeneralSettingsOptions()
	{
		createNewGroup("General");
		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_MOL_TITLE, SettingsModelBoolean.class),
				"With molecule title?"));
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_ATOM_COLOURS, SettingsModelBoolean.class),
				"With atom colours?"));
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_ATOM_NUMBERS, SettingsModelBoolean.class),
				"With atom numbers?"));

		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_CARBON_SYMBOLS, SettingsModelBoolean.class),
				"With carbon symbols?"));
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_TERMINAL_CARBONS, SettingsModelBoolean.class),
				"With terminal carbons?"));
	}

	private void createHighlightingOptions()
	{
		Class<? extends DataValue>[] allowedTypes = (Class<? extends DataValue>[]) new Class<?>[2];
		allowedTypes[0] = IntValue.class;
		allowedTypes[1] = (Class<? extends DataValue>) ListDataValue.class;
		
		createNewGroup("Highlights");
		setHorizontalPlacement(false);
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_CLEAR_HIGHLIGHT, SettingsModelBoolean.class),
				"Clear existing highlight?"));

		setHorizontalPlacement(true);

		atomHighlightSetting = settings.getSetting(DepictionSettings.CONFIG_HIGHIGHT_ATOMS, SettingsModelBoolean.class);

		atomHighlights = settings.getSetting(DepictionSettings.CONFIG_HIGHLIGHT_ATOMS_LIST,
				SettingsModelColumnName.class);

		atomHighlightSetting.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{

				atomHighlights.setEnabled(atomHighlightSetting.getBooleanValue());
				settings.getSetting(DepictionSettings.CONFIG_ATOM_COLOUR, SettingsModelColor.class)
						.setEnabled(atomHighlightSetting.getBooleanValue());
			}
		});

		addDialogComponent(new DialogComponentBoolean(atomHighlightSetting, "Highlight atoms"));

		DialogComponentColumnNameSelection atomHighlight = new DialogComponentColumnNameSelection(atomHighlights,
				"Atom index positions", 0, false, allowedTypes);

		addDialogComponent(atomHighlight);
		addDialogComponent(new DialogComponentColorChooser(
				settings.getSetting(DepictionSettings.CONFIG_ATOM_COLOUR, SettingsModelColor.class), "Colour", true));

		setHorizontalPlacement(false);
		setHorizontalPlacement(true);

		bondHighlightSetting = settings.getSetting(DepictionSettings.CONFIG_HIGHLIGHT_BONDS,
				SettingsModelBoolean.class);

		bondHighlights = settings.getSetting(DepictionSettings.CONFIG_HIGHLIGHT_BONDS_LIST,
				SettingsModelColumnName.class);

		bondHighlightSetting.addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent e)
			{
				bondHighlights.setEnabled(bondHighlightSetting.getBooleanValue());
				settings.getSetting(DepictionSettings.CONFIG_BOND_COLOUR, SettingsModelColor.class)
						.setEnabled(bondHighlightSetting.getBooleanValue());

			}
		});

		addDialogComponent(new DialogComponentBoolean(bondHighlightSetting, "Highlight bonds"));
		DialogComponentColumnNameSelection bondHighlight = new DialogComponentColumnNameSelection(bondHighlights,
				"Bond index positions", 0, false, allowedTypes);

		addDialogComponent(bondHighlight);
		addDialogComponent(new DialogComponentColorChooser(
				settings.getSetting(DepictionSettings.CONFIG_BOND_COLOUR, SettingsModelColor.class), "Colour", true));

		setHorizontalPlacement(false);
		setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentBoolean(
				settings.getSetting(DepictionSettings.CONFIG_WITH_OUTER_GLOW, SettingsModelBoolean.class),
				"With outer glow?"));

		addDialogComponent(new DialogComponentNumber(
				settings.getSetting(DepictionSettings.CONFIG_WITH_OUTER_GLOW_WIDTH, SettingsModelDouble.class),
				"Outer glow width?", 0.1d));
	}
}
