/*
 * Copyright (c) 2016 European Bioinformatics Institute (EMBL-EBI)
 *                    Stephan Beisken <beisken@ebi.ac.uk>
 *
 * Contact: cdk-devel@lists.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version. All we ask is that proper credit is given
 * for our work, which includes - but is not limited to - adding the above
 * copyright notice to the beginning of your source code files, and to any
 * copyright notice that you may distribute with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
 */
package org.openscience.cdk.formula.rules;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.HashMap;
import java.util.Map;

/**
 * A molecular formula filter that checks the element ratios (H/C and SiNOPSFClBr/C) of a molecular formula and
 * excludes formulas that are outside of the ratio range derived from the Wiley mass spectral database for the mass
 * range 30 Da - 1500 Da by Kind et al.
 *
 * <p/>
 *
 * Kind, T., & Fiehn, O. (2007). Seven Golden Rules for heuristic filtering of molecular formulas obtained by accurate
 * mass spectrometry. BMC Bioinformatics, 8, 105. doi:10.1186/1471-2105-8-105
 *
 * <p/>
 * zero carbons
 * including excluding boundaries
 * non valid elements
 */
public class ElementRatioRule implements IRule {

    public enum RatioType {
        HYDROGEN_CARBON, HETERATOMS_CARBON, ALL
    };

    public enum RatioRange {
        COMMON, EXTENDED, EXTREME
    };

    private RatioType ratioType = RatioType.HYDROGEN_CARBON;
    private RatioRange ratioRange = RatioRange.COMMON;
    private Map<String, double[]> range = commonRange();

    public ElementRatioRule(Object[] params) throws CDKException {
        setParameters(params);
    }

    @Override
    public void setParameters(Object[] params) throws CDKException {
        if (params.length != 2) {
            throw new CDKException("ElementRatioRule class expects exactly two parameters.");
        }

        if (params[0] != null && params[0] instanceof RatioType) {
            ratioType = (RatioType) params[0];
        } else {
            throw new CDKException("ElementRatioRule class expects the first parameter to be of type RatioType.");
        }

        if (params[1] != null && params[1] instanceof RatioRange) {
            ratioRange = (RatioRange) params[1];
            switch (ratioRange) {
                case COMMON:
                    range = commonRange();
                    break;
                case EXTENDED:
                    range = extendedRange();
                    break;
                case EXTREME:
                    range = extremeRange();
                    break;
                default:
                    throw new CDKException("No valid ratio range type: " + ratioType.name());
            }
        } else {
            throw new CDKException("ElementRatioRule class expects the second parameter to be of type RatioRange.");
        }
    }

    @Override
    public Object[] getParameters() {
        return new Object[] { ratioType, ratioRange };
    }

    @Override
    public double validate(IMolecularFormula formula) throws CDKException {
        double score = 1d;
        if (formula == null) {
            score = 0d;
        } else {
            // avoid integer division
            double nCarbon = MolecularFormulaManipulator.getElementCount(formula, "C");
            if (nCarbon == 0) {
                score = 1d;
            } else if (ratioType == RatioType.HYDROGEN_CARBON) {
                double nHydrogen = MolecularFormulaManipulator.getElementCount(formula, "H");
                double ratio = nHydrogen / nCarbon;
                if (ratioRange == RatioRange.EXTREME) {
                    double[] limits = range.get("H");
                    if (ratio < limits[0] || ratio >= limits[1] &&
                            !(ratio > limits[2] && ratio <= limits[3])) {
                        score = 0d;
                    }
                } else if (ratio < range.get("H")[0] || ratio > range.get("H")[1]) {
                    score = 0d;
                }
            } else if (ratioType == RatioType.HETERATOMS_CARBON) {
                for (IElement element : MolecularFormulaManipulator.elements(formula)) {
                    if (element.getSymbol().equals("H") || element.getSymbol().equals("C")) {
                        continue;
                    }
                    if (range.containsKey(element.getSymbol())) {
                        double nElement = MolecularFormulaManipulator.getElementCount(formula, element);
                        double ratio = nElement / nCarbon;
                        double[] limits = range.get(element.getSymbol());
                        if (ratio < limits[0] || ratio > limits[1]) {
                            score = 0d;
                        }
                    }
                }
            } else if (ratioType == RatioType.ALL) {
                for (IElement element : MolecularFormulaManipulator.elements(formula)) {
                    if (element.getSymbol().equals("C")) {
                        continue;
                    }
                    if (range.containsKey(element.getSymbol())) {
                        double nElement = MolecularFormulaManipulator.getElementCount(formula, element);
                        double ratio = nElement / nCarbon;
                        double[] limits = range.get(element.getSymbol());
                        if (ratioRange == RatioRange.EXTREME) {
                            if (ratio < limits[0] || ratio >= limits[1] &&
                                    !(ratio > limits[2] && ratio <= limits[3])) {
                                score = 0d;
                            }
                        } else if (ratio < limits[0] || ratio > limits[1]) {
                            score = 0d;
                        }
                    }
                }
            } else {
                throw new CDKException("Unknown ratio type: " + ratioType.name());
            }
        }
        return score;
    }

    private Map<String, double[]> commonRange() {
        Map<String, double[]> map = new HashMap<String, double[]>();

        map.put("H", new double[] {0.2, 3.1});
        map.put("F", new double[] {0, 1.5});
        map.put("Cl", new double[] {0, 0.8});
        map.put("Br", new double[] {0, 0.8});
        map.put("N", new double[] {0, 1.3});
        map.put("O", new double[] {0, 1.2});
        map.put("P", new double[] {0, 0.3});
        map.put("S", new double[] {0, 0.8});
        map.put("Si", new double[] {0, 0.5});

        return map;
    }

    private Map<String, double[]> extendedRange() {
        Map<String, double[]> map = new HashMap<String, double[]>();

        map.put("H", new double[] {0.1, 6});
        map.put("F", new double[] {0, 6});
        map.put("Cl", new double[] {0, 2});
        map.put("Br", new double[] {0, 2});
        map.put("N", new double[] {0, 4});
        map.put("O", new double[] {0, 3});
        map.put("P", new double[] {0, 2});
        map.put("S", new double[] {0, 3});
        map.put("Si", new double[] {0, 1});

        return map;
    }

    private Map<String, double[]> extremeRange() {
        Map<String, double[]> map = new HashMap<String, double[]>();

        map.put("H", new double[] {Double.MIN_VALUE, 0.1, 6, 9});
        map.put("F", new double[] {1.5, Double.MAX_VALUE});
        map.put("Cl", new double[] {0.8, Double.MAX_VALUE});
        map.put("Br", new double[] {0.8, Double.MAX_VALUE});
        map.put("N", new double[] {1.3, Double.MAX_VALUE});
        map.put("O", new double[] {1.2, Double.MAX_VALUE});
        map.put("P", new double[] {0.3, Double.MAX_VALUE});
        map.put("S", new double[] {0.8, Double.MAX_VALUE});
        map.put("Si", new double[] {0.5, Double.MAX_VALUE});

        return map;
    }
}
