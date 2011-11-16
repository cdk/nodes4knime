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
 * ---------------------------------------------------------------------
 *
 * History
 *   06.06.2007 (thor): created
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
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.knime.type.CDKCell;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;

/**
 * Molecule converter that is able to convert between MoSS graphs and CDK
 * molecules and the corresponding {@link CDKValue}. This class is thread safe.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class CDKMolConverter extends MolConverter<IAtomContainer, CDKValue> {
    /**
     * {@inheritDoc}
     */
    @Override
    public NamedGraph convert(final String name,
            final IAtomContainer description, final boolean active)
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
                    IsotopeFactory.getInstance(atom.getBuilder()).configure(
                            atom);
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
                throw new IllegalArgumentException("Unknown bond type: "
                        + b.getOrder());
            }
        }

        return g;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IMolecule convert(final Graph graph) {
        IChemObjectBuilder objectBuilder =
                NoNotificationChemObjectBuilder.getInstance();
        IMolecule mol = objectBuilder.newInstance(IMolecule.class);

        HashMap<Node, IAtom> map = new HashMap<Node, IAtom>();
        for (int i = 0; i < graph.getNodeCount(); i++) {
            Node node = graph.getNode(i);
            IAtom atom =
                    objectBuilder.newInstance(IAtom.class,
                            Atoms.getElemName(node.getType()));
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
                throw new IllegalArgumentException("Unknown bond type: "
                        + e.getType());
            }
        }

        return mol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedGraph convert(final String name, final CDKValue value,
            final boolean active) throws ParseException {
        return convert(name, value.getAtomContainer(), active);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell createCell(final Graph graph) {
        return new CDKCell(convert(graph));
    }
}
