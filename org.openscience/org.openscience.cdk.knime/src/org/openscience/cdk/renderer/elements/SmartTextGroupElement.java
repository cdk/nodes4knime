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
package org.openscience.cdk.renderer.elements;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class SmartTextGroupElement extends TextElement {

	/**
	 * Compass-point positions for text element annotation children.
	 * 
	 */
	public enum Position {
		NW, SW, SE, NE, N, S, W, E
	};

	// the background color for the text elements
	private Color backgroundColor = Color.WHITE;

	/**
	 * A string of text that should be shown around the parent.
	 * 
	 * @author maclean
	 * 
	 */
	public class Child {

		/**
		 * The text of this child.
		 */
		public final String text;

		/**
		 * A subscript (if any) for the child.
		 */
		public final String subscript;

		/**
		 * The position of the child relative to the parent.
		 */
		public final Position position;

		/**
		 * Make a child element with the specified text and position.
		 * 
		 * @param text the child's text
		 * @param position the position of the child relative to the parent
		 */
		public Child(String text, Position position) {
			this.text = text;
			this.position = position;
			this.subscript = null;
		}

		/**
		 * Make a child element with the specified text, subscript, and
		 * position.
		 * 
		 * @param text the child's text
		 * @param subscript a subscript for the child
		 * @param position the position of the child relative to the parent
		 */
		public Child(String text, String subscript, Position position) {
			this.text = text;
			this.position = position;
			this.subscript = subscript;
		}

	}

	/**
	 * The child text elements.
	 */
	public final List<Child> children;

	/**
	 * Make a text group at (x, y) with the text and color given.
	 * 
	 * @param x the x-coordinate of the center of the text
	 * @param y the y-coordinate of the center of the text
	 * @param text the text to render
	 * @param color the color of the text
	 */
	public SmartTextGroupElement(double x, double y, String text, Color color) {
		super(x, y, text, color);
		this.children = new ArrayList<Child>();
	}

	/**
	 * Add a child text element.
	 * 
	 * @param text the child text to add
	 * @param position the position of the child relative to this parent
	 */
	public void addChild(String text, Position position) {
		this.children.add(new Child(text, position));
	}

	/**
	 * Add a child text element with a subscript.
	 * 
	 * @param text the child text to add
	 * @param subscript a subscript for the child
	 * @param position the position of the child relative to the parent
	 */
	public void addChild(String text, String subscript, Position position) {
		this.children.add(new Child(text, subscript, position));
	}

	/**
	 * {@inheritDoc}
	 */
	public void accept(IRenderingVisitor v) {
		v.visit(this);
	}

	/**
	 * Sets the background color for the text elements.
	 * 
	 * @param color the color
	 */
	public void setBackgroundColor(Color color) {

		this.backgroundColor = color;
	}

	/**
	 * Returns the background color for the text elements.
	 * 
	 * @return the color
	 */
	public Color getBackgroundColor() {

		return backgroundColor;
	}
}
