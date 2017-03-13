package org.openscience.cdk.knime.type.snippets.java;

import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.SimpleDataCellToJavaConverterFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKAdapterCell;
import org.openscience.cdk.knime.type.CDKCell3;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * Allows access to a CDK Value in Java Snippets. As there is no CDKCellFactory this will
 * create a {@link CDKAdapterCell} with a {@link CDKCell3}. 
 * <br></br>
 * TODO: Create a cell factory!
 * 
 * @author Samuel Webb, Lhasa Limited
 * @since 1.5.700, 13/03/2017
 *
 */
public class CdkCellToJavaFactory extends SimpleDataCellToJavaConverterFactory<CDKValue, IAtomContainer> {
	public CdkCellToJavaFactory() {
		super(CDKValue.class, IAtomContainer.class, new DataCellToJavaConverter<CDKValue, IAtomContainer>() {
			@Override
			public IAtomContainer convert(CDKValue structure) throws Exception {
				return structure.getAtomContainer();
			}
		}, "IAtomContainer");
	}
}