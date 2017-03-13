package org.openscience.cdk.knime.type.snippets.java;

import org.knime.core.data.DataCell;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.SimpleJavaToDataCellConverterFactory;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.type.CDKAdapterCell;
import org.openscience.cdk.knime.type.CDKCell3;

/**
 * Converts an IAtomContainer to a CDK Cell. The implementation is not safe to
 * updates to CDK Cells as the choice in CDK Cell is currently hard coded. This
 * should be replaced with a CDK Cell Factory.
 * 
 * @author Samuel Webb, Lhasa Limited
 * @since 1.5.700, 13/03/2017
 */
public class JavaToCdkCellFactory extends SimpleJavaToDataCellConverterFactory<IAtomContainer> {
	// Constructor
	public JavaToCdkCellFactory() {
		super(IAtomContainer.class, CDKAdapterCell.RAW_TYPE, new JavaToDataCellConverter<IAtomContainer>() {
			@Override
			public DataCell convert(IAtomContainer structure) throws Exception {
				return new CDKAdapterCell(new CDKCell3(structure));
			}
		}, "IAtomContainer");

	}
}