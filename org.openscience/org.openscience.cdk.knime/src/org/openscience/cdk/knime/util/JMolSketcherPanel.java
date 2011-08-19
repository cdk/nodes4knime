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
 *   29.08.2007 (thor): created
 */
package org.openscience.cdk.knime.util;

import java.io.StringWriter;
import java.util.ArrayList;

import javax.vecmath.Vector2d;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.SMILESWriter;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.layout.TemplateHandler;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.ChemModelManipulator;
import org.openscience.jchempaint.JChemPaintPanel;

/**
 * This is a panel that lets the user draw structures and returns them as Smiles
 * strings. They can be loaded again into an empty panel afterwards.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class JMolSketcherPanel extends JChemPaintPanel {
    /**
     * Creates a new sketcher panel.
     */
    public JMolSketcherPanel() {
    	super(DefaultChemObjectBuilder.getInstance().
    			newInstance(IChemModel.class));
    	IChemModel chemModel = this.getChemModel();
    	chemModel.setMoleculeSet(chemModel.getBuilder().
    			newInstance(IMoleculeSet.class));
    	chemModel.getMoleculeSet().addAtomContainer(
    			chemModel.getBuilder().newInstance(IMolecule.class));
    	
        setShowMenuBar(false);
        setIsNewChemModel(true);        
    }

    /**
     * Loads the given structures into the panel.
     *
     * @param smiles a list of Smiles strings
     * @throws Exception if an exception occurs
     */
    public void loadStructures(final String... smiles) throws Exception {
        IChemModel chemModel = getChemModel();
        IMoleculeSet moleculeSet = new MoleculeSet();
        if (smiles != null && smiles.length > 0) {
            for (int i = 0; i < smiles.length; i++) {
                SmilesParser parser =
                        new SmilesParser(NoNotificationChemObjectBuilder
                                .getInstance());
                IMolecule m = parser.parseSmiles(smiles[i]);
                StructureDiagramGenerator sdg = new StructureDiagramGenerator();
                sdg.setTemplateHandler(new TemplateHandler(moleculeSet
                        .getBuilder()));
                sdg.setMolecule(m);
                sdg.generateCoordinates(new Vector2d(0, 1));
                m = sdg.getMolecule();
                moleculeSet.addAtomContainer(m);
                // if there are no atoms in the actual chemModel
                // all 2D-coordinates would be set to NaN
                if (ChemModelManipulator.getAtomCount(chemModel) != 0) {
                    IAtomContainer cont = chemModel.getBuilder().
                    	newInstance(IAtomContainer.class);
                    for (Object ac : ChemModelManipulator
                            .getAllAtomContainers(chemModel)) {
                        cont.add((IAtomContainer)ac);
                    }
                }
                chemModel.setMoleculeSet(moleculeSet);
            }
        }
        // this.fire
        // jchemPaintModel.fireChange(chemModel);
    }

    /**
     * Returns an array of Smiles strings for the drawn molecules.
     *
     * @return an array of Smiles strings
     */
    public String[] getAllSmiles() {
        IMoleculeSet s = getChemModel().getMoleculeSet();
        ArrayList<String> smiles = new ArrayList<String>();
        for (int i = 0; i < s.getAtomContainerCount(); i++) {
            StringWriter writer = new StringWriter();
            SMILESWriter w = new SMILESWriter(writer);
            w.writeMolecule(s.getMolecule(i));
            String sm = writer.toString().trim();
            if (sm.length() > 0) {
                smiles.add(sm);
            }
        }
        return smiles.toArray(new String[smiles.size()]);
    }
}
