/*
 * Copyright (C) 2003 - 2012 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.JMolViewerKNIMEUtils;
import org.jmol.viewer.Viewer;
import org.knime.chem.types.SdfValue;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerPanel extends JPanel {

	private final JmolViewer m_viewer;
	private final JmolAdapter m_adapter;
	private int m_lastZoomFactorInt = 50;

	private final static IAtomContainer FAIL_STRUCTURE = SilentChemObjectBuilder.getInstance().newInstance(
			IAtomContainer.class);

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

	/**
	 * Set a new cell being displayed.
	 * 
	 * @param value The cell to show. If missing or wrong type, nothing is shown.
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

		IAtomContainerSet moleculeSet = new AtomContainerSet();
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
