/* Copyright (C) 2003-2007  The Chemistry Development Kit (CDK) project
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *  */
package org.openscience.cdk.tools.manipulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IDoubleBondStereochemistry;
import org.openscience.cdk.interfaces.ILonePair;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.ISingleElectron;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.interfaces.ITetrahedralChirality;
import org.openscience.cdk.interfaces.IDoubleBondStereochemistry.Conformation;
import org.openscience.cdk.stereo.DoubleBondStereochemistry;
import org.openscience.cdk.stereo.TetrahedralChirality;

public class SmartAtomContainerManipulator {

	/**
     * Suppress any explicit hydrogens in the provided container. Only hydrogens
     * that can be represented as a hydrogen count value on the atom are
     * suppressed. The container is updated and no elements are copied, please
     * use either {@link #copyAndSuppressedHydrogens} if you would to preserve
     * the old instance.
     * 
     * @param org the container from which to remove hydrogens
     * @return the input for convenience
     * @see #copyAndSuppressedHydrogens 
     */
    public static IAtomContainer suppressNonChiralHydrogens(IAtomContainer org) {

        boolean anyHydrogenPresent = false;
        for (IAtom atom : org.atoms()) {
            if ("H".equals(atom.getSymbol())) {
                anyHydrogenPresent = true;
                break;
            }
        }
        
        if (!anyHydrogenPresent)
            return org;
        
        // we need fast adjacency checks (to check for suppression and 
        // update hydrogen counts)
        final int[][] graph = GraphUtil.toAdjList(org);
        
        final int nOrgAtoms = org.getAtomCount();
        final int nOrgBonds = org.getBondCount();
        
        int nCpyAtoms = 0;
        int nCpyBonds = 0;
        
        final Set<IAtom> hydrogens = new HashSet<IAtom>(nOrgAtoms); 
        final IAtom[]    cpyAtoms  = new IAtom[nOrgAtoms];
        
        // keep stereo hydrogens (direct or related to stereocenters)
        final Set<IAtom> keep = new HashSet<IAtom>(nOrgAtoms);
        for (IStereoElement se : org.stereoElements()) {
        	if (se instanceof ITetrahedralChirality) {
                ITetrahedralChirality tc = (ITetrahedralChirality) se;
                for (IAtom atom : tc.getLigands()) {
                	if (atom.getSymbol().equals("H")) {
                		keep.add(atom);
                	}
                }
        	} else if (se instanceof IDoubleBondStereochemistry) {
                IDoubleBondStereochemistry db = (IDoubleBondStereochemistry) se;
                for (IBond bond : db.getBonds()) {
                	for (int i = 0; i < bond.getAtomCount(); i++) {
                		IAtom atom = bond.getAtom(i);
                		if (atom.getSymbol().equals("H")) {
                			keep.add(atom);
                		}
                	}
                }
        	}
        }
        
        // filter the original container atoms for those that can/can't
        // be suppressed
        for (int v = 0; v < nOrgAtoms; v++) {
            final IAtom atom = org.getAtom(v);
            if (suppressibleHydrogen(org, graph, v) && !keep.contains(atom)) {
                hydrogens.add(atom);
                incrementImplHydrogenCount(org.getAtom(graph[v][0]));
            } else {
                cpyAtoms[nCpyAtoms++] = atom;
            }
        }
        
        // none of the hydrogens could be suppressed - no changes need to be made
        if (hydrogens.isEmpty())
            return org;

        org.setAtoms(Arrays.copyOf(cpyAtoms, nCpyAtoms));
        
        // we now update the bonds - we have auxiliary variable remaining that
        // bypasses the set membership checks if all suppressed bonds are found  
        IBond[] cpyBonds  = new IBond[nOrgBonds - hydrogens.size()];
        int     remaining = hydrogens.size(); 
        
        for (final IBond bond : org.bonds()) {
            if (remaining > 0 &&
                    (hydrogens.contains(bond.getAtom(0))
                            || hydrogens.contains(bond.getAtom(1)))) {
                remaining--;
                continue;
            }
            cpyBonds[nCpyBonds++] = bond;
        }
        
        // we know how many hydrogens we removed and we should have removed the
        // same number of bonds otherwise the containers is a strange
        if (nCpyBonds != cpyBonds.length)
            throw new IllegalArgumentException("number of removed bonds was less than the number of removed hydrogens");

        org.setBonds(cpyBonds);
        
        List<IStereoElement> elements = new ArrayList<IStereoElement>();

        
        for (IStereoElement se : org.stereoElements()) {
            if (se instanceof ITetrahedralChirality) {
                ITetrahedralChirality tc = (ITetrahedralChirality) se;
                IAtom   focus     = tc.getChiralAtom();
                IAtom[] neighbors = tc.getLigands();
                boolean updated   = false;
                for (int i = 0; i < neighbors.length; i++) {
                    if (hydrogens.contains(neighbors[i])) {
                        neighbors[i] = focus;
                        updated      = true;
                    }
                }
                
                // no changes
                if (!updated) {
                    elements.add(tc);
                } else {
                    elements.add(new TetrahedralChirality(focus, neighbors, tc.getStereo()));
                }
            } else if (se instanceof IDoubleBondStereochemistry) {
                IDoubleBondStereochemistry db = (IDoubleBondStereochemistry) se;
                Conformation conformation = db.getStereo();

                IBond orgStereo = db.getStereoBond();
                IBond orgLeft   = db.getBonds()[0];
                IBond orgRight  = db.getBonds()[1];

                // we use the following variable names to refer to the
                // double bond atoms and substituents
                // x       y
                //  \     /
                //   u = v 

                IAtom u = orgStereo.getAtom(0);
                IAtom v = orgStereo.getAtom(1);
                IAtom x = orgLeft.getConnectedAtom(u);
                IAtom y = orgRight.getConnectedAtom(v);
                
                // if xNew == x and yNew == y we don't need to find the
                // connecting bonds
                IAtom xNew = x;
                IAtom yNew = y;

                if (hydrogens.contains(x)) {
                    conformation = conformation.invert();
                    xNew = findOther(org, u, v, x);
                }

                if (hydrogens.contains(y)) {
                    conformation = conformation.invert();
                    yNew = findOther(org, v, u, y);
                }

                // no other atoms connected, invalid double-bond configuration?
                if (x == null || y == null)
                    continue;
                
                // no changes
                if (x == xNew && y == yNew) {
                    elements.add(db);
                    continue;
                }

                // XXX: may perform slow operations but works for now
                IBond cpyLeft  = xNew != x ? org.getBond(u, xNew) : orgLeft;
                IBond cpyRight = yNew != y ? org.getBond(v, yNew) : orgRight;

                elements.add(new DoubleBondStereochemistry(orgStereo,
                                                           new IBond[]{cpyLeft, cpyRight},
                                                           conformation));
            }
        }
        
        org.setStereoElements(elements);
        
        // single electron and lone pairs are not really used but we update 
        // them just in-case but we just use the inefficient AtomContainer
        // methods
        
        if (org.getSingleElectronCount() > 0) {
            Set<ISingleElectron> remove = new HashSet<ISingleElectron>();
            for (ISingleElectron se : org.singleElectrons()) {
                if (!hydrogens.contains(se.getAtom()))
                    remove.add(se);
            }
            for (ISingleElectron se : remove) {
                org.removeSingleElectron(se);
            }
        }

        if (org.getLonePairCount() > 0) {
            Set<ILonePair> remove = new HashSet<ILonePair>();
            for (ILonePair lp : org.lonePairs()) {
                if (!hydrogens.contains(lp.getAtom()))
                    remove.add(lp);
            }
            for (ILonePair lp : remove) {
                org.removeLonePair(lp);
            }
        }
        
        return org;
    }
    
    /**
     * Finds an neighbor connected to 'atom' which is not 'exclude1' 
     * or 'exclude2'. If no neighbor exists - null is returned.
     * 
     * @param container structure
     * @param atom      atom to find a neighbor of
     * @param exclude1  the neighbor should not be this atom
     * @param exclude2  the neighbor should also not be this atom  
     * @return a neighbor of 'atom', null if not found
     */
    private static IAtom findOther(IAtomContainer container, IAtom atom, IAtom exclude1, IAtom exclude2) {
        for (IAtom neighbor : container.getConnectedAtomsList(atom)) {
            if (neighbor != exclude1 && neighbor != exclude2)
                return neighbor;
        }
        return null;
    }
    
    /**
     * Is the {@code atom} a suppressible hydrogen and can be represented as
     * implicit. A hydrogen is suppressible if it is not an ion, not the major
     * isotope (i.e. it is a deuterium or tritium atom) and is not molecular
     * hydrogen.
     *
     * @param container the structure
     * @param graph     adjacent list representation
     * @param v         vertex (atom index)
     * @return the atom is a hydrogen and it can be suppressed (implicit)
     */
    private static boolean suppressibleHydrogen(final IAtomContainer container,
                                                final int[][] graph,
                                                final int v) {
        
        IAtom atom = container.getAtom(v);
        
        // is the atom a hydrogen
        if (!"H".equals(atom.getSymbol()))
            return false;
        // is the hydrogen an ion?
        if (atom.getFormalCharge() != null && atom.getFormalCharge() != 0)
            return false;
        // is the hydrogen deuterium / tritium?
        if (atom.getMassNumber() != null && atom.getMassNumber() != 1)
            return false;
        // hydrogen is either not attached to 0 or 2 neighbors
        if (graph[v].length != 1)
            return false;

        // okay the hydrogen has one neighbor, if that neighbor is not a 
        // hydrogen (i.e. molecular hydrogen) then we can suppress it
        return !"H".equals(container.getAtom(graph[v][0]).getSymbol());
    }
    
    /**
     * Increment the implicit hydrogen count of the provided atom. If the atom
     * was a non-pseudo atom and had an unset hydrogen count an exception is
     * thrown.
     * 
     * @param atom an atom to increment the hydrogen count of
     */
    private static void incrementImplHydrogenCount(final IAtom atom) {
        Integer hCount = atom.getImplicitHydrogenCount();
        
        if (hCount == null) {
            if (!(atom instanceof IPseudoAtom))
                throw new IllegalArgumentException("a non-pseudo atom had an unset hydrogen count");
            hCount = 0;
        }
        
        atom.setImplicitHydrogenCount(hCount + 1);
    }
}
