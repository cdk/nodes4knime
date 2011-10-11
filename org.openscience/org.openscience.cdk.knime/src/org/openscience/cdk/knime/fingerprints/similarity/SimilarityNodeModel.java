package org.openscience.cdk.knime.fingerprints.similarity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.AggregationMethod;
import org.openscience.cdk.knime.fingerprints.similarity.SimilaritySettings.FingerprintTypes;
import org.openscience.cdk.similarity.Tanimoto;

/**
 * This is the model implementation of the similarity node. CDK is used to
 * calculate the Tanimoto coefficient for two fingerprints. The minimum, maximum
 * or average can be selected as aggregation method.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class SimilarityNodeModel extends NodeModel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(SimilarityNodeModel.class);

	private final SimilaritySettings m_settings = new SimilaritySettings();

	/**
	 * Constructor for the node model.
	 */
	protected SimilarityNodeModel() {

		super(2, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		DataTableSpec spec = inData[0].getDataTableSpec();
		DataTableSpec specRef = inData[1].getDataTableSpec();
		
		String s = m_settings.fingerprintColumn();
		String sr = m_settings.fingerprintRefColumn();
		
		final int fingerprintColIndex = spec.findColumnIndex(s);
		final int fingerprintRefColIndex = specRef.findColumnIndex(sr);
		
		DataTableSpec newSpec = getTableSpec(spec);
    	BufferedDataContainer container = exec.createDataContainer(newSpec);
		
    	// create fingerprint reference map: fingerprint -> rowKeys[]
    	Map<BitSet, ArrayList<String>> fingerprintRefs = new HashMap<BitSet, ArrayList<String>>();
    	
    	int bitSetSize = 0;
    	if (m_settings.fingerprintType().equals(FingerprintTypes.MACCS)) {
    		bitSetSize = 166;
    	}
    	
    	for (DataRow row : inData[1]) {
    		if (row.getCell(fingerprintRefColIndex).isMissing()) {
    			setWarningMessage("Missing value in reference at row " + row.getKey().getString());
    			continue;
    		}
    		String bitString = ((BitVectorValue) row.getCell(fingerprintRefColIndex)).toBinaryString();
    		BitSet bs = new BitSet(bitSetSize);
    		StringBuilder sb = new StringBuilder();
    		sb.append(bitString);
    		bitString = sb.reverse().toString();
    		for (int j = 0; j < bitString.length(); j++) {
    			if (bitString.length() > j && bitString.charAt(j) == '1') bs.set(j);
    		}
    		if (fingerprintRefs.containsKey(bs)) {
    			fingerprintRefs.get(bs).add(row.getKey().getString());
    		} else {
    			ArrayList<String> keyList = new ArrayList<String>();
    			keyList.add(row.getKey().getString());
    			fingerprintRefs.put(bs, keyList);
    		}
    	}
    	
    	// row-wise calculation
    	for (DataRow row : inData[0]) {

			DataCell cell = row.getCell(fingerprintColIndex);
			if (row.getCell(fingerprintColIndex).isMissing()) {
    			setWarningMessage("Missing value in sample at row " + row.getKey().getString() + " - row skipped");
    			continue;
    		}
			String bitString = ((BitVectorValue) cell).toBinaryString();
			StringBuilder sb = new StringBuilder();
    		sb.append(bitString);
    		bitString = sb.reverse().toString();
    		BitSet bs = new BitSet(bitSetSize);
    		for (int j = 0; j < bitString.length(); j++) {
    			if (bitString.charAt(j) == '1') bs.set(j);
    		}
    		float coeff = 0.0f;
    		float pcoeff = 0.0f;
    		ArrayList<String> pkey = null;
    		Iterator<Map.Entry<BitSet, ArrayList<String>>> it = fingerprintRefs.entrySet().iterator();
    		if (m_settings.aggregationMethod().equals(AggregationMethod.Minimum)) {
    			pcoeff = 1;
    		    while (it.hasNext()) {
    		        Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
    		        coeff = Tanimoto.calculate(bs, pairs.getKey());
    		        if (coeff < pcoeff) {
    		        	pcoeff = coeff;
    		        	pkey = pairs.getValue();
    		        }
    		    }
    		} else if (m_settings.aggregationMethod().equals(AggregationMethod.Maximum)) {
    		    while (it.hasNext()) {
    		    	Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
    		        coeff = Tanimoto.calculate(bs, pairs.getKey());
    		        if (coeff > pcoeff) {
    		        	pcoeff = coeff;
    		        	pkey = (ArrayList<String>) pairs.getValue();
    		        }
    		    }
    		} else if (m_settings.aggregationMethod().equals(AggregationMethod.Average)) {
    		    while (it.hasNext()) {
    		    	Map.Entry<BitSet, ArrayList<String>> pairs = it.next();
    		        coeff += Tanimoto.calculate(bs, pairs.getKey());  
    		    }
    		    pcoeff = coeff / inData[1].getRowCount();
    		    pkey = new ArrayList<String>();
    		}
    		
    		List<DataCell> dataCells = new ArrayList<DataCell>();
			for (DataCell oldCell : row) {
				dataCells.add(oldCell);
			}
			DoubleCell dCell = new DoubleCell(pcoeff);
			String res = "";
			for (String st : pkey) {
				if (res.equals("")) {
					res += st;
				} else {
					res += "|" +st;
				}
			}
			StringCell sCell = new StringCell(res);
			dataCells.add(dCell);
			dataCells.add(sCell);
			container.addRowToTable(new DefaultRow(row.getKey(), dataCells));
    	}
    	container.close();
		return new BufferedDataTable[] { container.getTable() };
	}
	
	private DataTableSpec getTableSpec(DataTableSpec spec) {
		DataColumnSpec[] colOutSpec = new DataColumnSpec[spec.getNumColumns() + 2];
    	int i;
    	for (i = 0; i < spec.getNumColumns(); i++) {
    		colOutSpec[i] = spec.getColumnSpec(i);
		}
    	
    	String newColName = m_settings.aggregationMethod() + " aggregation";
		newColName = DataTableSpec.getUniqueColumnName(spec, newColName);		
    	
    	colOutSpec[i] = new DataColumnSpecCreator(newColName, DoubleCell.TYPE).createSpec();
    	colOutSpec[i+1] = new DataColumnSpecCreator("Reference", StringCell.TYPE).createSpec();
    	
    	return new DataTableSpec(colOutSpec);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// nothing to do;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		if (m_settings.fingerprintColumn() == null) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isCompatible(BitVectorValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.fingerprintColumn(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else {
				throw new InvalidSettingsException("No DenseBitVector compatible column in input table");
			}
		} else if (m_settings.fingerprintRefColumn() == null) {
			String name = null;
			for (DataColumnSpec s : inSpecs[0]) {
				if (s.getType().isCompatible(BitVectorValue.class)) {
					name = s.getName();
				}
			}
			if (name != null) {
				m_settings.fingerprintRefColumn(name);
				setWarningMessage("Auto configuration: Using column \"" + name + "\"");
			} else {
				throw new InvalidSettingsException("No reference DenseBitVector compatible column in input table");
			}
		}
		// create new table spec
		return new DataTableSpec[] { getTableSpec(inSpecs[0]) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_settings.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		SimilaritySettings s = new SimilaritySettings();
		s.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {
		// nothing to do;
	}
}
