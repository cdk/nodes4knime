/*
 * Copyright (c) 2013, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
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
package org.openscience.cdk.renderer.generators;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.vecmath.Point2d;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.annotations.TestMethod;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.color.SmartCPKAtomColors;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.elements.SmartTextGroupElement;
import org.openscience.cdk.renderer.elements.SmartTextGroupElement.Position;
import org.openscience.cdk.renderer.generators.SmartAtomNumberGenerator.WillDrawAtomNumbers;
import org.openscience.cdk.renderer.generators.parameter.AbstractGeneratorParameter;

public class SmartExtendedAtomGenerator extends BasicAtomGenerator {
	
	private final IAtomColorer colorer = new SmartCPKAtomColors();

	/** Boolean that indicates if implicit hydrogens should be depicted. */
	public static class ShowImplicitHydrogens extends AbstractGeneratorParameter<Boolean> {
		/** {@inheritDoc} */
		public Boolean getDefault() {
			return Boolean.TRUE;
		}
	}

	private IGeneratorParameter<Boolean> showImplicitHydrogens = new ShowImplicitHydrogens();

	/**
	 * Boolean that indicates if atom type names should be given instead of
	 * element symbols.
	 */
	public static class ShowAtomTypeNames extends AbstractGeneratorParameter<Boolean> {
		/** {@inheritDoc} */
		public Boolean getDefault() {
			return Boolean.FALSE;
		}
	}

	private ShowAtomTypeNames showAtomTypeNames = new ShowAtomTypeNames();

	/** {@inheritDoc} */
	@Override
	@TestMethod("testSingleAtom")
	public IRenderingElement generate(IAtomContainer container, IAtom atom, RendererModel model) {

		boolean drawNumbers = false;
		String drawSymbol = "";
		boolean drawSequential = true;

		if (model.hasParameter(WillDrawAtomNumbers.class)) {
			drawNumbers = model.getParameter(WillDrawAtomNumbers.class).getValue();
			if (model.hasParameter(SmartAtomNumberGenerator.DrawSpecificElement.class)) {
				drawSymbol = model.getParameter(SmartAtomNumberGenerator.DrawSpecificElement.class).getValue();
			}
			if (model.hasParameter(SmartAtomNumberGenerator.DrawSequential.class)) {
				drawSequential = model.getParameter(SmartAtomNumberGenerator.DrawSequential.class).getValue();
			}
		}

		if (!hasCoordinates(atom) || invisibleHydrogen(atom, model)
				|| (invisibleCarbon(atom, container, model) && !drawNumbers)) {
			return null;
		} else if (model.getParameter(CompactAtom.class).getValue()) {
			return this.generateCompactElement(atom, model);
		} else {
			String text;
			if (atom instanceof IPseudoAtom) {
				text = ((IPseudoAtom) atom).getLabel();
			} else if (drawNumbers && drawSequential) {

				if (drawSymbol.isEmpty()) {
					text = String.valueOf(container.getAtomNumber(atom) + 1);
				} else if (drawSymbol.equals(atom.getSymbol())) {
					text = String.valueOf(container.getAtomNumber(atom) + 1);
				} else {
					text = invisibleCarbon(atom, container, model) ? "" : atom.getSymbol();
				}

			} else if (drawNumbers) {
				if (drawSymbol.isEmpty()) {
					text = atom.getID();
				} else if (drawSymbol.equals(atom.getSymbol())) {
					text = atom.getID();
				} else {
					text = invisibleCarbon(atom, container, model) ? "" : atom.getSymbol();
				}

			} else {
				text = atom.getSymbol();
				if (text.equals("H") && atom.getMassNumber() != null) {
					if (atom.getMassNumber() == 2)
						text = "D";
					if (atom.getMassNumber() == 3)
						text = "T";
				}
			}
			Point2d point = atom.getPoint2d();
			
			// override default behaviour
			// Color ccolor = getAtomColor(atom, model);
			Color ccolor = colorer.getAtomColor(atom);

			// if invisible carbon
			if (text == null || text.isEmpty())
				return null;

			SmartTextGroupElement textGroup = new SmartTextGroupElement(point.x, point.y, text, ccolor);
			if (atom.getProperty(CDKConstants.ANNOTATIONS) != null) {
				textGroup.setBackgroundColor(new Color(atom.getProperty(CDKConstants.ANNOTATIONS, Integer.class)));
			}
			decorate(textGroup, container, atom, model);
			return textGroup;
		}
	}

	private void decorate(SmartTextGroupElement textGroup, IAtomContainer container, IAtom atom, RendererModel model) {
		Stack<Position> unused = getUnusedPositions(container, atom);

		Integer formalCharge = atom.getFormalCharge();
		if (formalCharge != null && formalCharge != 0) {

			String chargeString = "";
			if (formalCharge == 1) {
				chargeString = "+";
			} else if (formalCharge > 1) {
				chargeString = formalCharge + "+";
			} else if (formalCharge == -1) {
				chargeString = "-";
			} else if (formalCharge < -1) {
				int absCharge = Math.abs(formalCharge);
				chargeString = absCharge + "-";
			}

			textGroup.addChild(chargeString, Position.NE);
		}

		if (atom.getMassNumber() != null) {
			try {
				Integer atomNumber = atom.getMassNumber()
						- Isotopes.getInstance().getMajorIsotope(atom.getSymbol()).getMassNumber();
				if (atomNumber != 0 && !atom.getSymbol().equals("H")) {
					textGroup.addChild("", "" + atom.getMassNumber(), Position.NW);
				}
			} catch (IOException exception) {
				// fall through
			}
		}

		if (showImplicitHydrogens.getValue()) {
			if (atom.getImplicitHydrogenCount() != null) {
				int hCount = atom.getImplicitHydrogenCount();
				if (hCount > 0) {
					Position position = getNextPosition(unused);
					if (position == Position.NE || position == Position.SE || position == Position.NW
							|| position == Position.SW) {
						getLeastBusyPosition(container, atom);
					}
					if (hCount == 1) {
						textGroup.addChild("H", position);
					} else {
						textGroup.addChild("H", String.valueOf(hCount), position);
					}
				}
			}
		}

		if (container.getSingleElectronCount() == 0)
			return;
		if (container.getConnectedSingleElectronsCount(atom) > 0) {
			for (int i = 0; i < container.getConnectedSingleElectronsCount(atom); i++) {
				Position position = getNextPosition(unused);
				textGroup.addChild("..", position);
			}
		}
	}

	private Position getLeastBusyPosition(IAtomContainer container, IAtom atom) {

		double freq = 0;
		for (IAtom connectedAtom : container.getConnectedAtomsList(atom)) {
			Position pos = getPosition(atom, connectedAtom);
			switch (pos) {
			case N:
				freq++;
				break;
			case NE:
				freq++;
				break;
			case E:
				freq++;
				break;
			case SE:
				freq++;
				break;
			}
		}
		return (freq <= (container.getConnectedAtomsCount(atom) / 2)) ? Position.E : Position.W;
	}

	private Position getNextPosition(Stack<Position> unused) {
		if (unused.size() > 0) {
			return unused.pop();
		} else {
			return Position.N;
		}
	}

	private Stack<Position> getUnusedPositions(IAtomContainer container, IAtom atom) {
		Stack<Position> unused = new Stack<Position>();
		for (Position p : Position.values()) {
			unused.add(p);
		}

		for (IAtom connectedAtom : container.getConnectedAtomsList(atom)) {
			Position used = getPosition(atom, connectedAtom);
			if (unused.contains(used)) {
				unused.remove(used);
			}
		}
		return unused;
	}

	private Position getPosition(IAtom atom, IAtom connectedAtom) {
		Point2d pointA = atom.getPoint2d();
		Point2d pointB = connectedAtom.getPoint2d();

		double angle = Math.toDegrees(Math.atan2(pointB.y - pointA.y, pointB.x - pointA.x));

		if (angle < -137.5) { // +10
			return Position.W;
		} else if (angle < -112.5) {
			return Position.SW;
		} else if (angle < -67.5) {
			return Position.S;
		} else if (angle < -32.5) { // -10
			return Position.SE;
		} else if (angle < 32.5) { // +10
			return Position.E;
		} else if (angle < 67.5) {
			return Position.NE;
		} else if (angle < 112.5) {
			return Position.N;
		} else if (angle < 137.5) { // -10
			return Position.NW;
		} else {
			return Position.W;
		}
	}

	/** {@inheritDoc} */
	@Override
	@TestMethod("getParametersTest")
	public List<IGeneratorParameter<?>> getParameters() {
		List<IGeneratorParameter<?>> parameters = new ArrayList<IGeneratorParameter<?>>();
		parameters.add(showImplicitHydrogens);
		parameters.add(showAtomTypeNames);
		parameters.addAll(super.getParameters());
		return parameters;
	}
}
