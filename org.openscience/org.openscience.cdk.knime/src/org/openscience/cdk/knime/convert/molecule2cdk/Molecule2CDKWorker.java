package org.openscience.cdk.knime.convert.molecule2cdk;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.chem.types.CMLValue;
import org.knime.chem.types.InchiValue;
import org.knime.chem.types.Mol2Value;
import org.knime.chem.types.MolValue;
import org.knime.chem.types.SdfValue;
import org.knime.chem.types.SmilesValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.MolConverter;
import org.openscience.cdk.knime.type.CDKCell;

public class Molecule2CDKWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionContext exec;
	private final int columnIndex;
	private final double max;
	private final BufferedDataContainer bdc;
	private final Molecule2CDKSettings settings;
	private final MolConverter converter;

	public Molecule2CDKWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionContext exec, final int max, final BufferedDataContainer bdc, final MolConverter converter,
			final Molecule2CDKSettings settings) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.max = max;
		this.settings = settings;
		this.converter = converter;
		this.columnIndex = columnIndex;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		DataCell cell = row.getCell(columnIndex);
		if (!cell.isMissing()) {
			IAtomContainer mol = converter.convert(getNotation(cell));
			if (mol == null) {
				cell = DataType.getMissingCell();
			} else {
				cell = CDKCell.createCDKCell(mol);
			}
		}

		if (settings.replaceColumn()) {
			row = new ReplacedColumnsDataRow(row, cell, columnIndex);
		} else {
			row = new AppendedColumnRow(row, cell);
		}

		return row;
	}

	private String getNotation(final DataCell cell) {

		switch (converter.format()) {
		case SMILES:
			return ((SmilesValue) cell).getSmilesValue();
		case CML:
			if (cell instanceof CMLValue) {
				return ((CMLValue) cell).getCMLValue();
			} else if (cell instanceof XMLValue) {
				return ((XMLValue) cell).toString();
			}
		case INCHI:
			return ((InchiValue) cell).getInchiString();
		case MOL2:
			return ((Mol2Value) cell).getMol2Value();
		case SDF:
			return ((SdfValue) cell).getSdfValue();
		case MOL:
			return ((MolValue) cell).getMolValue();
		default:
			return ((StringValue) cell).getStringValue();
		}
	}

	@Override
	protected void processFinished(ComputationTask task) throws ExecutionException, CancellationException,
			InterruptedException {

		DataRow append = task.get();
		if (!append.getCell(columnIndex).isMissing()) {
			bdc.addRowToTable(append);
		}

		exec.setProgress(
				this.getFinishedCount() / max,
				this.getFinishedCount() + " (active/submitted: " + this.getActiveCount() + "/"
						+ (this.getSubmittedCount() - this.getFinishedCount()) + ")");

		try {
			exec.checkCanceled();
		} catch (CanceledExecutionException cee) {
			throw new CancellationException();
		}
	}
}
