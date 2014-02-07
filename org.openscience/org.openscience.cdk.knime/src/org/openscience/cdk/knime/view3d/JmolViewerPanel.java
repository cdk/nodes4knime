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
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JPanel;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.JMolViewerKNIMEUtils;
import org.jmol.viewer.Viewer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemModel;
import org.openscience.cdk.ChemSequence;
import org.openscience.cdk.interfaces.IAtomContainerSet;

/**
 * @author wiswedel, University of Konstanz
 */
public class JmolViewerPanel extends JPanel {

	private final JmolViewer viewer;
	private final JmolAdapter adapter;
	private int lastZoomFactorInt = 1;

	public JmolViewerPanel() {

		adapter = new CdkJmolAdapter();
		viewer = Viewer.allocateViewer(this, adapter);
	}

	private final Dimension currentSize = new Dimension();
	private final Rectangle rectClip = new Rectangle();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void paintComponent(final Graphics g) {

		super.paintComponent(g);
		getSize(currentSize);
		g.getClipBounds(rectClip);
		viewer.renderScreenImage(g, currentSize, rectClip);
		lastZoomFactorInt = viewer.getZoomPercent();
	}

	/**
	 * Set new molecules for display.
	 * 
	 * @param value The molecules to show. If missing or wrong type, nothing is shown.
	 */
	public void setMolecules(final IAtomContainerSet molecules) {
		
		ChemModel model = new ChemModel();
		model.setMoleculeSet(molecules);
		ChemSequence sequence = new ChemSequence();
		sequence.addChemModel(model);
		ChemFile chemFile = new ChemFile();
		chemFile.addChemSequence(sequence);

		synchronized (viewer) {
			viewer.openClientFile("", "", chemFile);
		}
		
		JMolViewerKNIMEUtils.zoomToPercent(viewer, lastZoomFactorInt);
		
		repaint();
	}
}
