package org.openscience.cdk.knime.nodes.depiction;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.knime.base.data.xml.SvgCellFactory;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.MissingCell;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.image.png.PNGImageCellFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.openscience.cdk.depict.DepictionGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.nodes.depiction.util.CdkSimpleStreamableFunctionNodeModel;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;

import com.google.common.primitives.Ints;

/**
 * This is the model implementation of Depiction. Depict CDK structures into
 * images
 *
 * @author Sameul Webb, Lhasa Limited
 */
public class DepictionNodeModel extends CdkSimpleStreamableFunctionNodeModel
{

	public static NodeLogger LOGGER = NodeLogger.getLogger(DepictionNodeModel.class);

	public DepictionNodeModel()
	{
		super();
		localSettings = new DepictionSettings();
	}

	private DepictionGenerator createDepictor()
	{
		DepictionSettings setup = ((DepictionSettings) localSettings);

		// Image

		int width = setup.getWidth();

		int height = setup.getHeight();

		DepictionGenerator dg = new DepictionGenerator().withSize(width, height);

		if (setup.withFillToFit())
			dg = dg.withFillToFit();

		// Highlighting
		if (setup.withOuterGlow())
			dg = dg.withOuterGlowHighlight().withOuterGlowHighlight(setup.getOuterGlowWidth());

		// General
		if (setup.withAtomColours())
			dg = dg.withAtomColors();

		if (setup.withAtomNumbers())
			dg = dg.withAtomNumbers();

		if (setup.withCarbonSymbols())
			dg = dg.withCarbonSymbols();

		if (setup.withMoleculeTitle())
			dg = dg.withMolTitle();

		if (setup.withTerminalCarbons())
			dg = dg.withTerminalCarbons();

		return dg;
	}

	@Override
	protected AbstractCellFactory createCellFactory(final DataTableSpec spec)
	{
		final DepictionGenerator dg = createDepictor();

		final DataType type = localSettings.getSetting(DepictionSettings.CONFIG_IMAGE_FORMAT, SettingsModelString.class)
				.getStringValue().equals("PNG") ? PNGImageCellFactory.TYPE : SvgCellFactory.TYPE;

		String colName = localSettings.getSetting(DepictionSettings.CONFIG_STRUCTURE_COLUMN, SettingsModelString.class)
				.getStringValue();

		DataColumnSpec imageColumnSpec = new DataColumnSpecCreator(
				DataTableSpec.getUniqueColumnName(spec, colName + " depiction"), type).createSpec();

		return new SingleCellFactory(true, imageColumnSpec)
		{

			@Override
			public DataCell getCell(DataRow row)
			{

				DataCell cell = null;
				try
				{
					CDKValue mol = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
					IAtomContainer con = CDKNodeUtils.getFullMolecule(mol.getAtomContainer());

					if (((DepictionSettings) localSettings).getClearHighlight())
					{
						for (int i = 0; i < con.getAtomCount(); i++)
						{
							con.getAtom(i).removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
						}

						for (int i = 0; i < con.getBondCount(); i++)
						{
							con.getBond(i).removeProperty(StandardGenerator.HIGHLIGHT_COLOR);
						}
					}

					if (localSettings.getSetting(DepictionSettings.CONFIG_HIGHIGHT_ATOMS, SettingsModelBoolean.class)
							.getBooleanValue())
					{
						int colIndex = spec
								.findColumnIndex(((DepictionSettings) localSettings).getAtomIndexColumnName());
						List<Integer> positions = getPositions(spec, row, colIndex);

						for (int position : positions)
						{
							try
							{
								con.getAtom(position).setProperty(StandardGenerator.HIGHLIGHT_COLOR, ((DepictionSettings) localSettings).getAtomColour());
							} catch (Exception e)
							{
								LOGGER.warn("Error highlighting structure for: " + row.getKey());
								throw new Exception("Problem with atom highlighting", e);
							}
						}

					}

					if (localSettings.getSetting(DepictionSettings.CONFIG_HIGHLIGHT_BONDS, SettingsModelBoolean.class)
							.getBooleanValue())
					{
						int colIndex = spec
								.findColumnIndex(((DepictionSettings) localSettings).getBondIndexColumnName());
						List<Integer> positions = getPositions(spec, row, colIndex);

						for (int position : positions)
						{
							try
							{
								con.getBond(position).setProperty(StandardGenerator.HIGHLIGHT_COLOR, ((DepictionSettings) localSettings).getBondColour());
							} catch (Exception e)
							{
								LOGGER.warn("Error highlighting structure for: " + row.getKey());
								throw new Exception("Problem with bond highlighting", e);
							}
						}
					}

					if (type.equals(PNGImageCellFactory.TYPE))
					{
						cell = createImageCell(dg.depict(con).toImg());
					} else
					{
						cell = new SvgCellFactory().createCell(dg.depict(con).toSvgStr());
					}

				} catch (Exception e)
				{
					LOGGER.error("Exception thrown generating image", e);
					// e.printStackTrace();
					cell = new MissingCell(e.getMessage());

					if (getWarningMessage() == null)
					{
						setWarningMessage(e.getMessage());
					} else
					{
						if (getWarningMessage().length() < 100
								&& !getWarningMessage().startsWith("Node executed with multiple warnings"))
						{
							setWarningMessage(getWarningMessage() + "\n" + e.getMessage());
						} else if (!getWarningMessage().startsWith("Node executed with multiple warnings"))
						{
							String message = getWarningMessage();
							setWarningMessage("Node executed with multiple warnings:\n" + message + "...");
						}
					}

				}

				return cell;
			}

			/**
			 * Get the highlighting positions, this could be from a normal
			 * IntValue or a ListCell containing multiple IntValue's to
			 * highlight
			 * 
			 * @param spec
			 * @param row
			 * @param colIndex
			 * @return
			 */
			private List<Integer> getPositions(DataTableSpec spec, DataRow row, int colIndex)
			{

				List<Integer> positions = new ArrayList<Integer>();
//				Integer[] positions = null;
				
				if (row.getCell(colIndex).isMissing())
				{
					
				}
				else if (spec.getColumnSpec(colIndex).getType().isCompatible(IntValue.class))
				{
					positions.add(((IntValue) row.getCell(colIndex)).getIntValue());
				} else if (spec.getColumnSpec(colIndex).getType().isCompatible(ListDataValue.class))
				{
					ListDataValue cell = (ListDataValue) row.getCell(colIndex);

					for (int i = 0; i < cell.size(); i++)
					{
						if (!cell.get(i).isMissing())
							positions.add(((IntValue) cell.get(i)).getIntValue());
					}

				} else
				{
					ListDataValue cell = (ListDataValue) row.getCell(colIndex);
					for (int i = 0; i < cell.size(); i++)
					{
						if (!cell.get(i).isMissing())
							positions.add(((IntValue) cell.get(i)).getIntValue());
					}

				}

				return positions;
//				return Ints.toArray(positions);
			}
		};
	}

	/**
	 * Creates an PNG cell from the given image
	 * 
	 * @param img
	 * @return
	 * @throws IOException
	 */
	private DataCell createImageCell(Image img) throws IOException
	{
		return PNGImageCellFactory.create(getImageAsBytes(img));
	}

	/**
	 * Convert the image to a ByteArray to be compatible with the
	 * {@link PNGImageCellFactory#create(byte[])} constructor
	 * 
	 * @param image
	 * @return
	 * @throws IOException
	 */
	private byte[] getImageAsBytes(Image image) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
		ImageIO.write((RenderedImage) image, "png", baos);
		baos.flush();
		baos.close();

		return baos.toByteArray();
	}

}
