/*
 * Copyright (C) 2003 - 2013 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
 * http://www.knime.org; Email: contact@knime.org
 *
 * This file is part of the KNIME CDK plugin.
 *
 * The KNIME CDK plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * The KNIME CDK plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with the plugin. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.knime.view3d;

import java.awt.Dimension;

import javax.swing.JMenuBar;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.chem.types.SdfValue;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * View that shows a table on top and the off structure at the bottom.
 *
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerNodeView extends NodeView<JmolViewerNodeModel> {
	private final TableView m_tableView;
	private final JmolViewerPanel m_panel;

	/**
	 * Inits view.
	 *
	 * @param model To get data from.
	 */
	public JmolViewerNodeView(final JmolViewerNodeModel model) {
		super(model);

		m_panel = new JmolViewerPanel();
		m_panel.setMinimumSize(new Dimension(200, 200));
		m_tableView = new TableView(model.getContentModel());
		ListSelectionModel selModel = m_tableView.getContentTable().getSelectionModel();
		selModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selModel.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(final ListSelectionEvent e) {

				ListSelectionModel sModel = (ListSelectionModel) e.getSource();
				int index = sModel.getMinSelectionIndex();
				setSelectedIndex(index);
			}
		});
		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		panel.add(m_tableView);
		panel.add(m_panel);
		JMenuBar menuBar = getJMenuBar();
		menuBar.add(m_tableView.createHiLiteMenu());
		menuBar.add(m_tableView.createViewMenu());
		setComponent(panel);
	}

	@Override
	protected JmolViewerNodeModel getNodeModel() {

		return super.getNodeModel();
	}

	@Override
	protected void modelChanged() {

		// nothing to do
	}

	@Override
	protected void onClose() {

		// nothing to do
	}

	@Override
	protected void onOpen() {

		// nothing to do
	}

	private void setSelectedIndex(final int index) {

		if (index < 0) {
			m_panel.setCDKValue(null);
		} else {
			JmolViewerNodeModel model = getNodeModel();

			String columnName = model.getSettings().molColumnName();
	        int column = model.getContentModel().getDataTable().getDataTableSpec().findColumnIndex(columnName);

			assert column >= 0;

			DataCell cell = model.getContentModel().getValueAt(index, column);
			// CDK method in JMolPanel broken: Does not display bonds and atom types
			if (cell instanceof SdfValue) {
				m_panel.setSDFValue(cell.isMissing() ? null : (SdfValue) cell);
			} else if (cell instanceof CDKValue) {
				m_panel.setCDKValue(cell.isMissing() ? null : (CDKValue) cell);
			}
		}
	}
}
