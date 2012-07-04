/*
 * Copyright (C) 2003 - 2011 University of Konstanz, Germany and KNIME GmbH, Konstanz, Germany Website:
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
package org.jmol.viewer;

import org.jmol.api.JmolViewer;

/**
 * Utility call to access package private methods in the JMOL library (most of the methods are package private, there is
 * no obvious way to access those methods other than creating such utility class). There is also almost no documentation
 * on JMol.
 * 
 * <p>
 * All this method does is to make some method in the JMOL package public, i.e. this class delegates method calls.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class JMolViewerKNIMEUtils {

	private JMolViewerKNIMEUtils() {

	}

	/**
	 * Calls on the JMolViewers transform manager the zoomToPercent method.
	 * 
	 * @param viewer The viewer
	 * @param percent The new zoom percent.
	 */
	public static void zoomToPercent(final JmolViewer viewer, final int percent) {

		if (viewer instanceof Viewer) {
			Viewer v = (Viewer) viewer;
			if (v.transformManager != null) {
				v.transformManager.zoomToPercent(percent);
			}
		}
	}

}
