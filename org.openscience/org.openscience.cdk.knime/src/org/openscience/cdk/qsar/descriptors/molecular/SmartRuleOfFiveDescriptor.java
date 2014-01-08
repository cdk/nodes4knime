/*  Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
 *
 *  Contact: cdk-devel@lists.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.qsar.descriptors.molecular;

import org.openscience.cdk.annotations.TestMethod;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerResult;

public class SmartRuleOfFiveDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {

	private static final String[] names = { "LipinskiFailures" };

	/**
	 * Constructor for the RuleOfFiveDescriptor object.
	 */
	public SmartRuleOfFiveDescriptor() {}

	/**
	 * Returns a <code>Map</code> which specifies which descriptor is
	 * implemented by this class.
	 * 
	 * These fields are used in the map:
	 * <ul>
	 * <li>Specification-Reference: refers to an entry in a unique dictionary
	 * <li>Implementation-Title: anything
	 * <li>Implementation-Identifier: a unique identifier for this version of
	 * this class
	 * <li>Implementation-Vendor: CDK, JOELib, or anything else
	 * </ul>
	 * 
	 * @return An object containing the descriptor specification
	 */
	@TestMethod("testGetSpecification")
	public DescriptorSpecification getSpecification() {
		return new DescriptorSpecification(
				"http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#lipinskifailures", this.getClass()
						.getName(), "$Id: 68c5736708116e708c5f0d7738055fe28ee57e3f $", "The Chemistry Development Kit");
	}

	/**
	 * Sets the parameters attribute of the RuleOfFiveDescriptor object.
	 * 
	 * There is only one parameter, which should be a Boolean indicating whether
	 * aromaticity should be checked or has already been checked. The name of
	 * the paramete is checkAromaticity.
	 * 
	 * @param params Parameter is only one: a boolean.
	 * @throws CDKException if more than 1 parameter or a non-Boolean parameter
	 *         is specified
	 * @see #getParameters
	 */
	@TestMethod("testSetParameters_arrayObject")
	public void setParameters(Object[] params) throws CDKException {
	}

	/**
	 * Gets the parameters attribute of the RuleOfFiveDescriptor object.
	 * 
	 * @return The parameters value
	 * @see #setParameters
	 */
	@TestMethod("testGetParameters")
	public Object[] getParameters() {
		return new Object[0];
	}

	@TestMethod(value = "testNamesConsistency")
	public String[] getDescriptorNames() {
		return names;
	}

	/**
	 * the method take a boolean checkAromaticity: if the boolean is true, it
	 * means that aromaticity has to be checked.
	 * 
	 * @param mol AtomContainer for which this descriptor is to be calculated
	 * @return The number of failures of the Lipinski rule
	 */
	@TestMethod("testCalculate_IAtomContainer")
	public DescriptorValue calculate(IAtomContainer mol) {

		int lipinskifailures = 0;
		IMolecularDescriptor xlogP = new SmartXLogPDescriptor();

		try {
			xlogP.setParameters(new Object[] {Boolean.FALSE});
			double xlogPvalue = ((DoubleResult) xlogP.calculate(mol).getValue()).doubleValue();

			IMolecularDescriptor acc = new SmartHBondAcceptorCountDescriptor();
			int acceptors = ((IntegerResult) acc.calculate(mol).getValue()).intValue();

			IMolecularDescriptor don = new HBondDonorCountDescriptor();
			int donors = ((IntegerResult) don.calculate(mol).getValue()).intValue();

			IMolecularDescriptor mw = new WeightDescriptor();
			Object[] mwparams = { "" };
			mw.setParameters(mwparams);
			double mwvalue = ((DoubleResult) mw.calculate(mol).getValue()).doubleValue();

			IMolecularDescriptor rotata = new RotatableBondsCountDescriptor();
			rotata.setParameters(new Object[] {Boolean.FALSE});
			int rotatablebonds = ((IntegerResult) rotata.calculate(mol).getValue()).intValue();

			if (xlogPvalue > 5.0) {
				lipinskifailures += 1;
			}
			if (acceptors > 10) {
				lipinskifailures += 1;
			}
			if (donors > 5) {
				lipinskifailures += 1;
			}
			if (mwvalue > 500.0) {
				lipinskifailures += 1;
			}
			if (rotatablebonds > 10.0) {
				lipinskifailures += 1;
			}
		} catch (CDKException e) {
			new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
					(int) Double.NaN), getDescriptorNames(), e);
		}

		return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
				lipinskifailures), getDescriptorNames());
	}

	/**
	 * Returns the specific type of the DescriptorResult object.
	 * <p/>
	 * The return value from this method really indicates what type of result
	 * will be obtained from the
	 * {@link org.openscience.cdk.qsar.DescriptorValue} object. Note that the
	 * same result can be achieved by interrogating the
	 * {@link org.openscience.cdk.qsar.DescriptorValue} object; this method
	 * allows you to do the same thing, without actually calculating the
	 * descriptor.
	 * 
	 * @return an object that implements the
	 *         {@link org.openscience.cdk.qsar.result.IDescriptorResult}
	 *         interface indicating the actual type of values returned by the
	 *         descriptor in the
	 *         {@link org.openscience.cdk.qsar.DescriptorValue} object
	 */
	@TestMethod("testGetDescriptorResultType")
	public IDescriptorResult getDescriptorResultType() {
		return new IntegerResult(1);
	}

	/**
	 * Gets the parameterNames attribute of the RuleOfFiveDescriptor object.
	 * 
	 * @return The parameterNames value
	 */
	@TestMethod("testGetParameterNames")
	public String[] getParameterNames() {
		String[] params = new String[0];
		return params;
	}

	/**
	 * Gets the parameterType attribute of the RuleOfFiveDescriptor object.
	 * 
	 * @param name The name of the parameter. In this case it is
	 *        'checkAromaticity'.
	 * @return An Object of class equal to that of the parameter being requested
	 */
	@TestMethod("testGetParameterType_String")
	public Object getParameterType(String name) {
		return false;
	}
}
