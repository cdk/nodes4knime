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
package org.knime.ext.chem.moss;

import java.text.ParseException;
import java.util.HashMap;

import moss.Atoms;
import moss.Bonds;
import moss.Edge;
import moss.Graph;
import moss.NamedGraph;
import moss.Node;
import moss.SMILES;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * Molecule converter that is able to convert between MoSS graphs and CDK molecules and the corresponding
 * {@link CDKValue}. This class is thread safe.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDKMolConverter extends MolConverter<IAtomContainer, CDKValue> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedGraph convert(final String name, final IAtomContainer description, final boolean active)
			throws ParseException {

		NamedGraph g;
		if (active) {
			// SLN is only a placeholder
			g = new NamedGraph(new SMILES(), name, 1, 0);
		} else {
			g = new NamedGraph(new SMILES(), name, 1, 1);
		}

		HashMap<IAtom, Integer> map = new HashMap<IAtom, Integer>();
		for (int i = 0; i < description.getAtomCount(); i++) {
			IAtom atom = description.getAtom(i);
			if (atom.getAtomicNumber() == 0) {
				try {
					IsotopeFactory.getInstance(atom.getBuilder()).configure(atom);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
			map.put(atom, g.addNode(atom.getAtomicNumber()));
		}
		for (int i = 0; i < description.getBondCount(); i++) {
			IBond b = description.getBond(i);
			int nodeA = map.get(b.getAtom(0));
			int nodeB = map.get(b.getAtom(1));

			if (b.getFlag(CDKConstants.ISAROMATIC)) {
				g.addEdge(nodeA, nodeB, Bonds.AROMATIC);
			} else if (b.getOrder() == CDKConstants.BONDORDER_SINGLE) {
				g.addEdge(nodeA, nodeB, Bonds.SINGLE);
			} else if (b.getOrder() == CDKConstants.BONDORDER_DOUBLE) {
				g.addEdge(nodeA, nodeB, Bonds.DOUBLE);
			} else if (b.getOrder() == CDKConstants.BONDORDER_TRIPLE) {
				g.addEdge(nodeA, nodeB, Bonds.TRIPLE);
			} else {
				throw new IllegalArgumentException("Unknown bond type: " + b.getOrder());
			}
		}

		return g;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IAtomContainer convert(final Graph graph) {

		IChemObjectBuilder objectBuilder = SilentChemObjectBuilder.getInstance();
		IAtomContainer mol = objectBuilder.newInstance(IAtomContainer.class);

		HashMap<Node, IAtom> map = new HashMap<Node, IAtom>();
		for (int i = 0; i < graph.getNodeCount(); i++) {
			Node node = graph.getNode(i);
			IAtom atom = objectBuilder.newInstance(IAtom.class, Atoms.getElemName(node.getType()));
			atom.setAtomicNumber(Atoms.getElem(node.getType()));
			map.put(node, atom);
			mol.addAtom(atom);
		}

		for (int i = 0; i < graph.getEdgeCount(); i++) {
			Edge e = graph.getEdge(i);

			IAtom atomA = map.get(e.getSource());
			IAtom atomB = map.get(e.getDest());
			IBond bond = objectBuilder.newInstance(IBond.class, atomA, atomB);
			mol.addBond(bond);

			if (Bonds.getBond(e.getType()) == Bonds.SINGLE) {
				bond.setOrder(CDKConstants.BONDORDER_SINGLE);
			} else if (Bonds.getBond(e.getType()) == Bonds.DOUBLE) {
				bond.setOrder(CDKConstants.BONDORDER_DOUBLE);
			} else if (Bonds.getBond(e.getType()) == Bonds.TRIPLE) {
				bond.setOrder(CDKConstants.BONDORDER_TRIPLE);
			} else if (Bonds.getBond(e.getType()) == Bonds.AROMATIC) {
				bond.setOrder(CDKConstants.BONDORDER_SINGLE);
				bond.setFlag(CDKConstants.ISAROMATIC, true);
				atomA.setFlag(CDKConstants.ISAROMATIC, true);
				atomB.setFlag(CDKConstants.ISAROMATIC, true);
			} else {
				throw new IllegalArgumentException("Unknown bond type: " + e.getType());
			}
		}

		return mol;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NamedGraph convert(final String name, final CDKValue value, final boolean active) throws ParseException {

		return convert(name, value.getAtomContainer(), active);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DataCell createCell(final Graph graph) {

		return new CDKCell(convert(graph));
	}
	
	@Override
	public DataType getDataType() {
	    return CDKCell.TYPE;
	}
}
