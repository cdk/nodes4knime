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
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.JMolViewerKNIMEUtils;
import org.jmol.viewer.Viewer;
import org.knime.chem.types.SdfValue;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.nonotify.NNMolecule;

/**
 *
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerPanel extends JPanel {

//    private static final NodeLogger LOGGER =
//        NodeLogger.getLogger(JMolViewerPanel.class);

    private final JmolViewer m_viewer;
    private final JmolAdapter m_adapter;
    private int m_lastZoomFactorInt = 100;

    private final static NNMolecule FAIL_STRUCTURE = new NNMolecule();

    public JmolViewerPanel() {
      m_adapter = new CdkJmolAdapter();
      m_viewer = Viewer.allocateViewer(this, m_adapter);
    }

    private final Dimension m_currentSize = new Dimension();
    private final Rectangle m_rectClip = new Rectangle();

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
      getSize(m_currentSize);
      g.getClipBounds(m_rectClip);
      m_viewer.renderScreenImage(g, m_currentSize, m_rectClip);
      m_lastZoomFactorInt = m_viewer.getZoomPercent();
    }


    /** Set a new cell being displayed.
     * @param value The cell to show. If missing or wrong type, nothing is
     * shown.
     */
    public void setCDKValue(final CDKValue value) {
        if (value == null) {
            setMol(FAIL_STRUCTURE);
        } else {
            setMol(value.getAtomContainer());
        }
        repaint();
    }

    public void setSDFValue(final SdfValue value) {
        if (value == null) {
            setMol(FAIL_STRUCTURE);
        } else {
            synchronized (m_viewer) {
                m_viewer.openStringInline(value.getSdfValue());
                JMolViewerKNIMEUtils.zoomToPercent(m_viewer, m_lastZoomFactorInt);
            }
        }
    }

    private void setMol(final IAtomContainer mol) {
        MoleculeSet moleculeSet = new MoleculeSet();
        moleculeSet.addAtomContainer(mol);
        ChemModel model = new ChemModel();
        model.setMoleculeSet(moleculeSet);
        ChemSequence sequence = new ChemSequence();
        sequence.addChemModel(model);
        ChemFile chemFile = new ChemFile();
        chemFile.addChemSequence(sequence);

        synchronized (m_viewer) {
            m_viewer.openClientFile("", "", chemFile);
        }
        JMolViewerKNIMEUtils.zoomToPercent(m_viewer, m_lastZoomFactorInt);
    }

}
