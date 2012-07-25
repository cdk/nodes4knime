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
package org.openscience.cdk.knime.type.renderer;

import java.awt.Color;
import java.util.List;

import javax.vecmath.Point2d;

import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.jchempaint.renderer.RendererModel;
import org.openscience.jchempaint.renderer.elements.ElementGroup;
import org.openscience.jchempaint.renderer.elements.IRenderingElement;
import org.openscience.jchempaint.renderer.elements.TextElement;
import org.openscience.jchempaint.renderer.generators.IGenerator;
import org.openscience.jchempaint.renderer.generators.IGeneratorParameter;

/**
 * Custom element number generator to display the number ids of all or selected atom groups.
 * 
 * @author Stephan Beisken
 */
public class ElementNumberGenerator implements IGenerator {

	public enum TYPE {
		ALL_ATOMS, C_ATOMS, H_ATOMS
	};

	private TYPE elementType;

	public ElementNumberGenerator() {

		this.elementType = TYPE.ALL_ATOMS;
	}
	
	public void setType(TYPE elementType) {
		
		this.elementType = elementType;
	}

	public IRenderingElement generate(IAtomContainer ac, RendererModel model) {

		ElementGroup numbers = new ElementGroup();
		if (!model.drawNumbers())
			return numbers;

		switch (elementType) {

		case ALL_ATOMS:
			addNumbers(numbers, ac);
			break;
		case C_ATOMS:
			addNumbers(numbers, ac, "C");
			break;
		case H_ATOMS:
			addNumbers(numbers, ac, "H");
			break;
		}

		return numbers;
	}

	private void addNumbers(ElementGroup numbers, IAtomContainer ac, String symbol) {

		for (IAtom atom : ac.atoms()) {

			if (atom.getSymbol().equals(symbol)) {
				Point2d p = atom.getPoint2d();
				numbers.add(new TextElement(p.x, p.y, atom.getID(), Color.BLACK));
			}
		}
	}

	private void addNumbers(ElementGroup numbers, IAtomContainer ac) {

		for (IAtom atom : ac.atoms()) {

			Point2d p = atom.getPoint2d();
			numbers.add(new TextElement(p.x, p.y, atom.getID(), Color.BLACK));
		}
	}

	@SuppressWarnings("rawtypes")
	public List<IGeneratorParameter> getParameters() {

		// do nothing
		return null;
	}
}
