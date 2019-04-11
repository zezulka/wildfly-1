/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.ejb.transaction.bmttocmt;

import javax.ejb.EJB;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.transaction.client.ContextTransactionManager;

@RunWith(Arquillian.class)
public class BeanManagedToContainerManagedTransactionsTestCase {

    @EJB(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    @EJB
    private BMTStateless bmtStateless;

    @Deployment
    public static JavaArchive deploy() {
        return ShrinkWrap.create(JavaArchive.class, "bmttocmt-test.jar")
            .addPackage(BeanManagedToContainerManagedTransactionsTestCase.class.getPackage());
    }

    /**
     * Transaction timeout - once set, this timeout value is effective until setTransactionTimeout
     * is invoked again with a different value.
     * The transaction timeout is held on the thread.
     */
    @Test
    public void testTimeout() throws SystemException {
        int transactionTimeoutToSet = 42;
        tm.setTransactionTimeout(transactionTimeoutToSet);
        Assert.assertEquals("Expecting transaction timeout has to be the same as it was written by setter",
            transactionTimeoutToSet, getTransactionTimeout(tm));
        bmtStateless.callBean();
        Assert.assertEquals("The transaction timeout has to be the same as before BMT call",
            transactionTimeoutToSet, getTransactionTimeout(tm));
    }

    public static int getTransactionTimeout(TransactionManager tmTimeout) {
        if (tmTimeout instanceof ContextTransactionManager) {
            return ((ContextTransactionManager) tmTimeout).getTransactionTimeout();
        }
        throw new IllegalStateException("Cannot get transaction timeout");
    }
}
