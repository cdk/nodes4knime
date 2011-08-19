/* Created on Dec 8, 2006 10:39:37 AM by thor
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
 * ------------------------------------------------------------------- *
 */
package org.openscience.cdk.knime.convert;

import java.util.concurrent.LinkedBlockingQueue;

import org.knime.core.node.KNIMEConstants;

/**
 * Thread pool for supporting time-consuming CDK operations.
 * <p>
 * Note: This class is subject to change without notice.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class TimeoutThreadPool {

    private final String m_name;

    /**
     * Creates a new thread pool.
     */
    public TimeoutThreadPool() {
        this("CDK");
    }

    /**
     * Creates a new thread pool with a name prefix for all created threads.
     *
     * @param name the threads' name prefix
     */
    public TimeoutThreadPool(final String name) {
        m_name = name;
    }

    private static class Worker extends Thread {
        private Runnable m_runnable;

        private final Object m_lock = new Object();

        private static int id = 0;

        Worker(final String name) {
            super(name + " " + id++);
            setPriority(Thread.MIN_PRIORITY);
            start();
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (m_lock) {
                    if (m_runnable == null) {
                        try {
                            m_lock.wait(5000);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    if (m_runnable != null) {
                        try {
                            m_runnable.run();
                        } catch (ThreadDeath d) {
                            break;
                        } finally {
                            synchronized (m_runnable) {
                                m_runnable.notifyAll();
                                m_runnable = null;
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        boolean doWork(final Runnable r, final int timeout)
                throws InterruptedException {
            synchronized (r) {
                synchronized (m_lock) {
                    m_runnable = r;
                    m_lock.notifyAll();
                }
                r.wait(timeout);
            }
            return (m_runnable == null);
        }
    }

    private final LinkedBlockingQueue<Worker> m_workers =
            new LinkedBlockingQueue<Worker>(KNIMEConstants.GLOBAL_THREAD_POOL
                    .getMaxThreads());

    /**
     * Runs the runnable in a separate thread and kills the thread after
     * <tt>timeout</tt> ms if it has not finished.
     *
     * @param r any runnable
     * @param timeout the timeout in ms
     * @return <code>true</code> if the runnable finished its execution before
     *         the thread was stopped, <code>false</code> otherwise
     * @throws InterruptedException if this thread was interrupted while waiting
     */
    public boolean run(final Runnable r, final int timeout)
            throws InterruptedException {
        Worker w = m_workers.poll();
        if ((w == null) || (!w.isAlive())) {
            w = new Worker(m_name);
        }

        if (!w.doWork(r, timeout)) {
            w.stop();
            return false;
        } else {
            m_workers.offer(w);
            return true;
        }
    }
}
