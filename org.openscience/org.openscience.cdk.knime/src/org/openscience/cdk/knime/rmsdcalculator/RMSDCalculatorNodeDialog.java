package org.openscience.cdk.knime.rmsdcalculator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.openscience.cdk.knime.rmsdcalculator.RMSDCalculatorSettings.AlignmentTypes;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * <code>NodeDialog</code> for the "RMSDCalculator" Node.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows creation of a simple dialog with standard
 * components. If you need a more complex dialog please derive directly from {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Luis F. de Figueiredo
 */
public class RMSDCalculatorNodeDialog extends NodeDialogPane {

	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_molColumn = new ColumnSelectionComboxBox((Border) null, CDKValue.class);

	private final RMSDCalculatorSettings m_settings = new RMSDCalculatorSettings();
	private final JRadioButton m_kabschalign = new JRadioButton("Kabsch algorithm");
	private final JRadioButton m_isomorphicalign = new JRadioButton("Isomorphic");

	/**
	 * New pane for configuring RMSDCalculator node dialog. This is just a suggestion to demonstrate possible default
	 * dialog components.
	 */
	public RMSDCalculatorNodeDialog() {

		JPanel p = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;

		p.add(new JLabel("Column with molecules   "), c);
		c.gridx++;
		p.add(m_molColumn, c);

		c.gridy++;
		c.gridx = 0;
		p.add(new JLabel("Alignment type   "), c);
		c.gridx = 1;
		p.add(m_kabschalign, c);
		c.gridy++;
		p.add(m_isomorphicalign, c);

		ButtonGroup bg = new ButtonGroup();
		bg.add(m_kabschalign);
		bg.add(m_isomorphicalign);
		addTab("RMSD Options", p);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
			throws NotConfigurableException {

		m_settings.loadSettingsForDialog(settings);

		m_molColumn.update(specs[0], m_settings.molColumnName());
		if (m_settings.alignmentType().equals(AlignmentTypes.Kabsch)) {
			m_kabschalign.setSelected(true);
		} else if (m_settings.alignmentType().equals(AlignmentTypes.Isomorphic)) {
			m_isomorphicalign.setSelected(true);
		}

	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {

		// TODO Auto-generated method stub
		m_settings.molColumnName(m_molColumn.getSelectedColumn());
		if (m_kabschalign.isSelected()) {
			m_settings.alignmentType(AlignmentTypes.Kabsch);
		} else if (m_isomorphicalign.isSelected()) {
			m_settings.alignmentType(AlignmentTypes.Isomorphic);
		}
		m_settings.saveSettings(settings);

	}
}
