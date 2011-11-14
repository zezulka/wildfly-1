/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Author : Jaikiran Pai
 */
@Stateless
@TransactionManagement(value = TransactionManagementType.BEAN)
@TransactionTimeout(value = BMTBean.SECONDS_TIMEOUT)
public class BMTBean implements TxActivity {
    private static final Logger logger = Logger.getLogger(BMTBean.class);

    static final int SECONDS_TIMEOUT = 3;

    @Resource(mappedName = "java:jboss/datasources/ExampleDS")
    private DataSource ds;

    public void doTxStuff() {
        try {
            int WAIT_SECONDS_IN_MILLIS = (SECONDS_TIMEOUT + 2) * 1000;
            logger.info("Sleeping for " + WAIT_SECONDS_IN_MILLIS + " milli. seconds for transaction timeout to happen");
            // sleep for a few seconds longer than the timeout
            Thread.sleep(WAIT_SECONDS_IN_MILLIS);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
        logger.info("Woke up after a nap! Now time to do a tx activity");
        // now do some dummy tx activity
        // If the bean was (incorrectly) invoked through a CMT tx context then the tx will have
        // timedout and we'll run into an exception while doing the tx activity and the test will fail.
        // However, if the bean was (correctly) invoked as a BMT context then this tx activity will go through fine
        Connection conn = null;
        try {
            conn = ds.getConnection();
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException sqle) {
                    // ignore
                    logger.debug("Ignoring exception that occured during connection close: ", sqle);
                }
            }
        }
    }
}
