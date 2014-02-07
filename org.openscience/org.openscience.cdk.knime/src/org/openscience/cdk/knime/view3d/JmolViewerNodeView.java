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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuBar;
import javax.swing.JSplitPane;

import org.knime.core.data.DataCell;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.silent.AtomContainerSet;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * View that shows a table on top and the off structure at the bottom.
 * 
 * @author wiswedel, University of Konstanz
 * @author Stephan Beisken, EMBL-EBI
 */
public class JmolViewerNodeView extends NodeView<JmolViewerNodeModel> {

	private final TableView tableView;
	private final JmolViewerPanel panel;
	
	private final static IAtomContainer FAIL_STRUCTURE = SilentChemObjectBuilder.getInstance().newInstance(
			IAtomContainer.class);

	/**
	 * Inits view.
	 * 
	 * @param model To get data from.
	 */
	public JmolViewerNodeView(final JmolViewerNodeModel model) {
		super(model);

		panel = new JmolViewerPanel();
		panel.setMinimumSize(new Dimension(300, 300));
		tableView = new TableView(model.getContentModel());
		tableView.getContentTable().addMouseListener(new MouseAdapter() {

			public void mouseReleased(MouseEvent e) {

				int[] indices = tableView.getContentTable().getSelectedRows();
				setIndices(indices);
			}
		});

		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		panel.add(this.panel);
		panel.add(tableView);
		panel.setResizeWeight(0.8);
		panel.setDividerLocation(200);
		
		JMenuBar menuBar = getJMenuBar();
		menuBar.add(tableView.createHiLiteMenu());
		menuBar.add(tableView.createViewMenu());
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

	private void setIndices(final int[] indices) {

		IAtomContainerSet molecules = new AtomContainerSet();
		
		if (indices.length > 0) {
			JmolViewerNodeModel model = getNodeModel();

			String columnName = model.getSettings().molColumnName();
			int column = model.getContentModel().getDataTable().getDataTableSpec().findColumnIndex(columnName);

			assert column >= 0;

			for (int index : indices) {
				DataCell cell = model.getContentModel().getValueAt(index, column);

				if (cell instanceof CDKValue) {
					molecules.addAtomContainer(cell.isMissing() ? FAIL_STRUCTURE : ((CDKValue) cell).getAtomContainer());
				}
			}
		}
		
		panel.setMolecules(molecules);
	}
}
