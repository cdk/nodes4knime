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
import java.util.Arrays;
import java.util.List;

import javax.vecmath.Vector2d;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.color.IAtomColorer;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.generators.parameter.AbstractGeneratorParameter;

public class SmartAtomNumberGenerator implements IGenerator<IAtomContainer> {

	/** Color to draw the atom numbers with. */
	public static class AtomNumberTextColor extends AbstractGeneratorParameter<Color> {
		/** {@inheritDoc} */
		public Color getDefault() {
			return Color.BLACK;
		}
	}

	private IGeneratorParameter<Color> textColor = new AtomNumberTextColor();

	/**
	 * Boolean parameter indicating if atom numbers should be drawn, allowing
	 * this feature to be disabled temporarily.
	 */
	public static class WillDrawAtomNumbers extends AbstractGeneratorParameter<Boolean> {
		/** {@inheritDoc} */
		public Boolean getDefault() {
			return Boolean.TRUE;
		}
	}

	private WillDrawAtomNumbers willDrawAtomNumbers = new WillDrawAtomNumbers();

	/**
	 * String parameter indicating which element (symbol) should be drawn.
	 */
	public static class DrawSpecificElement extends AbstractGeneratorParameter<String> {
		/**
		 * {@inheritDoc}
		 */
		public String getDefault() {
			return "-";
		}
	}

	private DrawSpecificElement drawSpecificElement = new DrawSpecificElement();

	/**
	 * Boolean parameter indicating if sequential or canonical numbering should
	 * be used.
	 */
	public static class DrawSequential extends AbstractGeneratorParameter<Boolean> {
		/**
		 * {@inheritDoc}
		 */
		public Boolean getDefault() {
			return Boolean.TRUE;
		}
	}

	private DrawSequential drawSequential = new DrawSequential();

	/**
	 * The color scheme by which to color the atom numbers, if the
	 * {@link ColorByType} boolean is true.
	 */
	public static class AtomColorer extends AbstractGeneratorParameter<IAtomColorer> {
		/** {@inheritDoc} */
		public IAtomColorer getDefault() {
			return new CDK2DAtomColors();
		}
	}

	private IGeneratorParameter<IAtomColorer> atomColorer = new AtomColorer();

	/** Boolean to indicate of the {@link AtomColorer} scheme will be used. */
	public static class ColorByType extends AbstractGeneratorParameter<Boolean> {
		/** {@inheritDoc} */
		public Boolean getDefault() {
			return Boolean.FALSE;
		}
	}

	private IGeneratorParameter<Boolean> colorByType = new ColorByType();

	/**
	 * Offset vector in screen space coordinates where the atom number label
	 * will be placed.
	 */
	public static class Offset extends AbstractGeneratorParameter<Vector2d> {
		/** {@inheritDoc} */
		public Vector2d getDefault() {
			return new Vector2d();
		}
	}

	private Offset offset = new Offset();

	/** {@inheritDoc} */
	@Override
	public IRenderingElement generate(IAtomContainer container, RendererModel model) {
		
		ElementGroup numbers = new ElementGroup();
		
		// empty generate method -- class only referenced for parameter reflection
		// now handled in the SmartExtendedAtomGenerator
		
		return numbers;
	}

	/** {@inheritDoc} */
	@Override
	public List<IGeneratorParameter<?>> getParameters() {
		return Arrays.asList(new IGeneratorParameter<?>[] {
				textColor,
				willDrawAtomNumbers,
				drawSpecificElement,
				drawSequential,
				offset,
				atomColorer,
				colorByType });
	}

}
