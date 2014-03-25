package org.openscience.cdk.knime.nodes.fingerprints;

import java.util.BitSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.AdapterValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.MultiThreadWorker;
import org.openscience.cdk.fingerprint.EStateFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.knime.commons.CDKNodeUtils;
import org.openscience.cdk.knime.nodes.fingerprints.FingerprintSettings.FingerprintTypes;
import org.openscience.cdk.knime.type.CDKValue;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

public class FingerprintWorker extends MultiThreadWorker<DataRow, DataRow> {

	private final ExecutionMonitor exec;
	private final double max;
	private final int columnIndex;
	private final BufferedDataContainer bdc;
	private final FingerprintSettings settings;

	public FingerprintWorker(final int maxQueueSize, final int maxActiveInstanceSize, final int columnIndex,
			final ExecutionMonitor exec, final int max, final BufferedDataContainer bdc,
			final FingerprintSettings settings) {

		super(maxQueueSize, maxActiveInstanceSize);
		this.exec = exec;
		this.bdc = bdc;
		this.max = max;
		this.settings = settings;
		this.columnIndex = columnIndex;
	}

	@Override
	protected DataRow compute(DataRow row, long index) throws Exception {

		final IFingerprinter fp;
		FingerprintTypes fpType = settings.fingerprintType();
		if (fpType.equals(FingerprintTypes.Extended)) {
			fp = new ExtendedFingerprinter();
		} else if (fpType.equals(FingerprintTypes.EState)) {
			fp = new EStateFingerprinter();
		} else if (fpType.equals(FingerprintTypes.Pubchem)) {
			fp = new PubchemFingerprinter(SilentChemObjectBuilder.getInstance());
		} else if (fpType.equals(FingerprintTypes.MACCS)) {
			fp = new MACCSFingerprinter();
		} else {
			fp = new Fingerprinter();
		}

		DataCell outCell;
		if (row.getCell(columnIndex).isMissing()
				|| (((AdapterValue) row.getCell(columnIndex)).getAdapterError(CDKValue.class) != null)) {
			outCell = DataType.getMissingCell();
		} else {
			CDKValue mol = ((AdapterValue) row.getCell(columnIndex)).getAdapter(CDKValue.class);
			try {
				BitSet fingerprint;
				IAtomContainer con = CDKNodeUtils.getExplicitClone(mol.getAtomContainer());
				fingerprint = fp.getBitFingerprint(con).asBitSet();
				// transfer the bitset into a dense bit vector
				DenseBitVector bitVector = new DenseBitVector(fingerprint.size());
				for (int i = fingerprint.nextSetBit(0); i >= 0; i = fingerprint.nextSetBit(i + 1)) {
					bitVector.set(i);
				}
				DenseBitVectorCellFactory fact = new DenseBitVectorCellFactory(bitVector);
				outCell = fact.createDataCell();
			} catch (Exception ex) {
				outCell = DataType.getMissingCell();
			}
		}

		return new AppendedColumnRow(row, outCell);
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
