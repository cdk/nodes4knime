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
 * -------------------------------------------------------------------
 *
 * History
 *   03.04.2005 (bernd): created
 */
package org.openscience.cdk.knime.type;

import org.knime.base.data.replace.ReplacedCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeLogger;


/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringToCDKConverter extends ReplacedCellFactory {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(StringToCDKConverter.class);

    /** Singleton to be used. */
    public static final StringToCDKConverter INSTANCE =
        new StringToCDKConverter();

    private StringToCDKConverter() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getReplacement(final DataRow row, final int column) {
        DataCell cell = row.getCell(column);
        if (cell.isMissing()) {
            return DataType.getMissingCell();
        }
        String smiles = ((StringValue)cell).getStringValue();
        return stringToDataCell(smiles);
    }

    @SuppressWarnings("deprecation")
    public DataCell stringToDataCell(final String smiles) {
        /*
         * Implementation detail: There is bug in the CDK code that keeps
         * parsing some smiles forever. I was talking to the Christoph
         * Steinbeck, one of the CDK gurus, it seems to be a nasty problem and
         * they are going to fix it "some time". The bug URL is
         * http://sourceforge.net/tracker/index.php?func=detail
         * &aid=1296113&group_id=20024&atid=120024.
         *
         * The only resolution that I see here (and it was also proposed by
         * Christoph Steinbeck, is to start the parsing in a thread and to time
         * out the parsing. Ordinary interrupt() invocations, however, fail as
         * the isInterrupted() seems not to be checked in the cdk code. I make
         * use of the deprecated stop() method here.
         */
        SmilesParserThread t = new SmilesParserThread(smiles);
        t.start();
        try {
            t.join(5000);
        } catch (InterruptedException ie) {
            LOGGER.debug("Caught InterruptedException, return missing cell.",
                    ie);
            return DataType.getMissingCell();
        }
        if (t.isAlive()) {
            try {
                // TODO fix with cdk bug #1296113
                t.stop();
            } catch (ThreadDeath td) {
                // silently ignored. Don't hit me. I don't know better.
            }
        }
        return t.getResult();
    }

    private static class SmilesParserThread extends Thread {

        private final String m_smiles;

        private DataCell m_result;

        SmilesParserThread(final String smiles) {
            m_smiles = smiles;
            m_result = DataType.getMissingCell();
        }

        DataCell getResult() {
            return m_result;
        }

        @Override
        public void run() {
            try {
                m_result = CDKCell.newInstance(m_smiles);
            } catch (IllegalArgumentException iae) {
                LOGGER.warn(
                        "Unable to convert to smiles: \"" + m_smiles + "\"",
                        iae);
                m_result = DataType.getMissingCell();
            }
        }
    }
}
