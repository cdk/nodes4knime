package org.openscience.cdk.knime.nodes.depiction;

import java.awt.Color;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.knime.nodes.depiction.util.NodeSettingCollection;

/**
 * Node specific settings for {@link DepictionNodeModel}
 * <br></br>
 * The atom and bond column selections start of disabled
 * 
 * @author Samuel Webb, Lhasa Limited
 * @since KNIME 3.1
 * @since CDK 1.5.600 ?
 *
 */
public class DepictionSettings extends NodeSettingCollection
{
	
	public static String CONFIG_IMAGE_FORMAT = "cfgImageFormat";
	public static String CONFIG_IMAGE_WIDTH = "cfgImageWidth";
	public static String CONFIG_IMAGE_HEIGHT = "cfgImageHeight";
	public static String CONFIG_WITH_FILL_TO_FIT = "cfgWithFillToFit";
	
	
	
	// Highlighting
	public static String CONFIG_HIGHIGHT_ATOMS = "cfgHighlightAtoms";
	public static String CONFIG_HIGHLIGHT_ATOMS_LIST = "cfgHighlightAtomsList";
	public static String CONFIG_CLEAR_HIGHLIGHT = "cfgClearHighlight";
	public static String CONFIG_WITH_OUTER_GLOW = "cfgWithOuterGlow";
	public static String CONFIG_WITH_OUTER_GLOW_WIDTH = "cfgWithOuterGlowWidth";
	public static String CONFIG_HIGHLIGHT_BONDS = "cfgHighlightBonds";
	public static String CONFIG_HIGHLIGHT_BONDS_LIST = "cfgHighlightBondsList";
	
	public static String CONFIG_ATOM_COLOUR = "cfgAtomColour";
	public static String CONFIG_BOND_COLOUR  = "cfgBondColour";

	// General
	public static String CONFIG_WITH_ATOM_COLOURS = "cfgWithAtomColours";
	public static String CONFIG_WITH_ATOM_NUMBERS = "cfgWithAtomNumbers";
	public static String CONFIG_WITH_CARBON_SYMBOLS = "cfgWithCarbonSymbols";
	public static String CONFIG_WITH_MOL_TITLE = "cfgWithMolTitle";
	public static String CONFIG_WITH_TERMINAL_CARBONS = "cfgWithTerminalCarbons";
	
	
	// Reaction
	public static String CONFIG_WITH_ATOM_MAP_HIGHLIGHT = "cfgWithAtomMapHighlight";
	public static String CONFIG_WITH_ATOM_MAP_NUMBERS = "cfgWithAtomMapNumbers";
	public static String CONFIG_WITH_REACTION_TITLE = "cfgWithReactionTitle";
	
	public DepictionSettings()
	{
		super();
	}
	

	@Override
	protected void addSettings()
	{
		
		settingMap.put(CONFIG_STRUCTURE_COLUMN, new SettingsModelColumnName(CONFIG_STRUCTURE_COLUMN, ""));
		
		// Image settings
		settingMap.put(CONFIG_IMAGE_FORMAT, new SettingsModelString(CONFIG_IMAGE_FORMAT, "PNG"));
		settingMap.put(CONFIG_IMAGE_WIDTH, new SettingsModelInteger(CONFIG_IMAGE_WIDTH, 250));
		settingMap.put(CONFIG_IMAGE_HEIGHT, new SettingsModelInteger(CONFIG_IMAGE_HEIGHT, 250));
		settingMap.put(CONFIG_WITH_FILL_TO_FIT, new SettingsModelBoolean(CONFIG_WITH_FILL_TO_FIT, true));
		
		// Highlighting
		settingMap.put(CONFIG_CLEAR_HIGHLIGHT, new SettingsModelBoolean(CONFIG_CLEAR_HIGHLIGHT, true));
		settingMap.put(CONFIG_HIGHIGHT_ATOMS, new SettingsModelBoolean(CONFIG_HIGHIGHT_ATOMS, false));
		
		SettingsModelColumnName atomHighlight = new SettingsModelColumnName(CONFIG_HIGHLIGHT_ATOMS_LIST, "");
		atomHighlight.setEnabled(false);
		settingMap.put(CONFIG_HIGHLIGHT_ATOMS_LIST, atomHighlight);
		settingMap.put(CONFIG_HIGHLIGHT_BONDS, new SettingsModelBoolean(CONFIG_HIGHLIGHT_BONDS, false));
		
		SettingsModelColumnName bondHighlight = new SettingsModelColumnName(CONFIG_HIGHLIGHT_BONDS_LIST, "");
		bondHighlight.setEnabled(false);
		
		settingMap.put(CONFIG_HIGHLIGHT_BONDS_LIST, bondHighlight);
		settingMap.put(CONFIG_WITH_OUTER_GLOW, new SettingsModelBoolean(CONFIG_WITH_OUTER_GLOW, true));
		settingMap.put(CONFIG_WITH_OUTER_GLOW_WIDTH, new SettingsModelDouble(CONFIG_WITH_OUTER_GLOW_WIDTH, 2d));
		
		settingMap.put(CONFIG_ATOM_COLOUR, new SettingsModelColor(CONFIG_ATOM_COLOUR, Color.RED));
		getSetting(CONFIG_ATOM_COLOUR, SettingsModelColor.class).setEnabled(false);
		settingMap.put(CONFIG_BOND_COLOUR, new SettingsModelColor(CONFIG_BOND_COLOUR, Color.RED));
		getSetting(CONFIG_BOND_COLOUR, SettingsModelColor.class).setEnabled(false);
		
		// General
		settingMap.put(CONFIG_WITH_ATOM_COLOURS, new SettingsModelBoolean(CONFIG_WITH_ATOM_COLOURS, true));
		settingMap.put(CONFIG_WITH_ATOM_NUMBERS, new SettingsModelBoolean(CONFIG_WITH_ATOM_NUMBERS, false));
		settingMap.put(CONFIG_WITH_CARBON_SYMBOLS, new SettingsModelBoolean(CONFIG_WITH_CARBON_SYMBOLS, false));
		
		SettingsModelBoolean molTitle = new SettingsModelBoolean(CONFIG_WITH_MOL_TITLE, false);
		molTitle.setEnabled(false);
		settingMap.put(CONFIG_WITH_MOL_TITLE, molTitle);
		settingMap.put(CONFIG_WITH_TERMINAL_CARBONS, new SettingsModelBoolean(CONFIG_WITH_TERMINAL_CARBONS, false));
	}
	
	protected Color getAtomColour()
	{
		return getSetting(CONFIG_ATOM_COLOUR, SettingsModelColor.class).getColorValue();
	}
	
	protected Color getBondColour()
	{
		return getSetting(CONFIG_BOND_COLOUR, SettingsModelColor.class).getColorValue();
	}
	
	protected String getAtomIndexColumnName()
	{
		return getSetting(CONFIG_HIGHLIGHT_ATOMS_LIST, SettingsModelColumnName.class).getColumnName();
	}
	
	protected int getWidth()
	{
		return getSetting(CONFIG_IMAGE_WIDTH, SettingsModelInteger.class).getIntValue();
	}
	
	protected int getHeight()
	{
		return getSetting(CONFIG_IMAGE_HEIGHT, SettingsModelInteger.class).getIntValue();
	}
	
	protected boolean getClearHighlight()
	{
		return getSetting(CONFIG_CLEAR_HIGHLIGHT, SettingsModelBoolean.class).getBooleanValue();
	}


	public boolean withAtomColours()
	{
		return getSetting(CONFIG_WITH_ATOM_COLOURS, SettingsModelBoolean.class).getBooleanValue();
	}


	public boolean withFillToFit()
	{
		return getSetting(CONFIG_WITH_FILL_TO_FIT, SettingsModelBoolean.class).getBooleanValue();
	}


	public boolean withOuterGlow()
	{
		return getSetting(CONFIG_WITH_OUTER_GLOW, SettingsModelBoolean.class).getBooleanValue();
	}


	public double getOuterGlowWidth()
	{
		return getSetting(CONFIG_WITH_OUTER_GLOW_WIDTH, SettingsModelDouble.class).getDoubleValue();
	}


	public boolean withAtomNumbers()
	{
		return getSetting(CONFIG_WITH_ATOM_NUMBERS, SettingsModelBoolean.class).getBooleanValue();
	}


	public boolean withCarbonSymbols()
	{
		return getSetting(CONFIG_WITH_CARBON_SYMBOLS, SettingsModelBoolean.class).getBooleanValue();
		
	}


	public boolean withMoleculeTitle()
	{
		return getSetting(CONFIG_WITH_MOL_TITLE, SettingsModelBoolean.class).getBooleanValue();
		
	}


	public boolean withTerminalCarbons()
	{
		return getSetting(CONFIG_WITH_TERMINAL_CARBONS, SettingsModelBoolean.class).getBooleanValue();
		
	}


	public String getBondIndexColumnName()
	{
		return getSetting(CONFIG_HIGHLIGHT_BONDS_LIST, SettingsModelColumnName.class).getColumnName();
	}


}
