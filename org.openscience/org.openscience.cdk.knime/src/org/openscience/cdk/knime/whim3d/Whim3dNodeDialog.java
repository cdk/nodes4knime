package org.openscience.cdk.knime.whim3d;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * <code>NodeDialog</code> for the "Whim3d" Node. Holistic descriptors described by Todeschini et al. The descriptors
 * are based on a number of atom weightings. There are 5 different possible weightings implemented.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Stephan Beisken
 */
public class Whim3dNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

	private final JCheckBox schemeUnitWeights = new JCheckBox("", true);
	private final JCheckBox schemeAtomicMasses = new JCheckBox();
	private final JCheckBox schemeVdWVolumes = new JCheckBox();
	private final JCheckBox schemeAtomicElectronneg = new JCheckBox();
	private final JCheckBox schemeAtomicPolariz = new JCheckBox();

	private Whim3dSettings settings = new Whim3dSettings();

	/**
	 * New pane for configuring the Whim3d node.
	 */
	protected Whim3dNodeDialog() {

		JPanel panel = new JPanel(new GridLayout(2, 1));
		panel.setBorder(BorderFactory.createTitledBorder("Settings"));

		GridBagConstraints c = createGridBagConstraints();

		JPanel sourcePanel = createSourcePanel(c);
		JPanel parameterPanel = createParameterPanel(c);

		panel.add(sourcePanel);
		panel.add(parameterPanel);

		this.addTab("Settings", panel);
	}

	/**
	 * Creates the constraints used for the layout.
	 */
	private GridBagConstraints createGridBagConstraints() {

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		return c;
	}

	/**
	 * Creates the source panel containing for input column selection.
	 */
	private JPanel createSourcePanel(GridBagConstraints c) {

		JPanel sourcePanel = new JPanel(new GridBagLayout());

		sourcePanel.add(new JLabel("CDK molecule column   "), c);
		c.gridx++;
		sourcePanel.add(molColumn, c);
		c.gridy++;
		c.gridx = 0;

		return sourcePanel;
	}

	/**
	 * Creates the parameter panel where different weighting schemes can be selected.
	 */
	private JPanel createParameterPanel(GridBagConstraints c) {

		JPanel parameterPanel = new JPanel(new GridBagLayout());
		parameterPanel.setBorder(BorderFactory.createTitledBorder("Weighting Schemes"));

		c.gridx = 0;
		c.gridy = 0;

		parameterPanel.add(new JLabel("Unit weights   "), c);
		c.gridx++;
		parameterPanel.add(schemeUnitWeights, c);
		c.gridy++;
		c.gridx = 0;

		parameterPanel.add(new JLabel("Atomic masses   "), c);
		c.gridx++;
		parameterPanel.add(schemeAtomicMasses, c);
		c.gridy++;
		c.gridx = 0;

		parameterPanel.add(new JLabel("Van der Waals volumes   "), c);
		c.gridx++;
		parameterPanel.add(schemeVdWVolumes, c);
		c.gridy++;
		c.gridx = 0;

		parameterPanel.add(new JLabel("Mulliken atomic electronegativites   "), c);
		c.gridx++;
		parameterPanel.add(schemeAtomicElectronneg, c);
		c.gridy++;
		c.gridx = 0;

		parameterPanel.add(new JLabel("Atomic polarizabilities   "), c);
		c.gridx++;
		parameterPanel.add(schemeAtomicPolariz, c);
		c.gridy++;
		c.gridx = 0;

		return parameterPanel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		loadSettings(settings);

		molColumn.update(specs[0], this.settings.getMolColumnName());

		schemeUnitWeights.setSelected(this.settings.isSchemeUnitWeights());
		schemeAtomicMasses.setSelected(this.settings.isSchemeAtomicMasses());
		schemeVdWVolumes.setSelected(this.settings.isSchemeVdWVolumes());
		schemeAtomicElectronneg.setSelected(this.settings.isSchemeAtomicElectronneg());
		schemeAtomicPolariz.setSelected(this.settings.isSchemeAtomicPolariz());
	}

	/**
	 * Loads the node settings into the custom Whim3d settings.
	 */
	private void loadSettings(NodeSettingsRO settings) {

		try {
			this.settings.loadSettings(settings);
		} catch (InvalidSettingsException ex) {
			// ignore it
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {

		this.settings.setMolColumnName(molColumn.getSelectedColumn());

		this.settings.setSchemeUnitWeights(schemeUnitWeights.isSelected());
		this.settings.setSchemeAtomicMasses(schemeAtomicMasses.isSelected());
		this.settings.setSchemeVdWVolumes(schemeVdWVolumes.isSelected());
		this.settings.setSchemeAtomicElectronneg(schemeAtomicElectronneg.isSelected());
		this.settings.setSchemeAtomicPolariz(schemeAtomicPolariz.isSelected());

		this.settings.saveSettings(settings);
	}
}
