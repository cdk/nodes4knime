/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 23, 2006 (wiswedel): created
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


/** View that shows a table on top and the off structure at the bottom.
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerNodeView extends NodeView<JmolViewerNodeModel> {

    private final TableView m_tableView;
    private final JmolViewerPanel m_panel;

    final static int UNKNOWN_TYPE = -1;
    final static int CDK_TYPE = 0;
    final static int SDF_TYPE = 1;

    /**
     * Inits view.
     * @param model To get data from.
     */
    public JmolViewerNodeView(final JmolViewerNodeModel model) {
        super(model);
        m_panel = new JmolViewerPanel();
        m_panel.setMinimumSize(new Dimension(200, 200));
        m_tableView = new TableView(model.getContentModel());
        ListSelectionModel selModel =
            m_tableView.getContentTable().getSelectionModel();
        selModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selModel.addListSelectionListener(new ListSelectionListener() {
           @Override
        public void valueChanged(final ListSelectionEvent e) {
               ListSelectionModel sModel = (ListSelectionModel)e.getSource();
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

    }

    @Override
    protected void onClose() {
    }

    @Override
    protected void onOpen() {
    }

    private void setSelectedIndex(final int index) {
        if (index < 0) {
            m_panel.setCDKValue(null);
        } else {
            JmolViewerNodeModel model = getNodeModel();
            int structureIndex = model.getStructureColumn();
            int structureType = model.getStructureType();
            assert structureIndex >= 0;
            DataCell cell = model.getContentModel().getValueAt(
                    index, structureIndex);
            switch (structureType) {
            case CDK_TYPE:
                m_panel.setCDKValue(cell.isMissing() ? null : (CDKValue)cell);
                break;
            case SDF_TYPE:
                m_panel.setSDFValue(cell.isMissing() ? null : (SdfValue)cell);
                break;
            case UNKNOWN_TYPE:
                m_panel.setCDKValue(null);
                break;
            }
        }
    }

}
