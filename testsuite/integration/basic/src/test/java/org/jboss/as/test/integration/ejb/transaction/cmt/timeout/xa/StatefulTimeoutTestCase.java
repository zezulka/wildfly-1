/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.cmt.timeout.xa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.transactions.TransactionTestLookupUtil;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.NoSuchEJBException;
import javax.ejb.SessionSynchronization;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;

/**
 * Tests on transaction timeout behavior with Stateful beans.
 * It's checked if synchronization is called correctly and
 * if transaction is aborted.
 */
@RunWith(Arquillian.class)
public class StatefulTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    private TransactionCheckerSingleton checker;


    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateful-txn-timeout.jar")
            .addPackage(StatefulTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addClasses(TimeoutUtil.class)
            .addAsManifestResource(new StringAsset("<beans></beans>"),  "beans.xml");
        return jar;
    }

    @Before
    public void startUp() throws NamingException {
        this.checker = TransactionTestLookupUtil.lookupModule(initCtx, TransactionCheckerSingleton.class);
        checker.resetAll();
    }

    /**
     * Having two successful call to SFSB bean where two {@link XAResource}s are enlisted
     * and 2PC is processed. No timeout happens.
     * <p>
     * The other checks verifies that synchronization callback methods enlisted to transaction
     * in bean code programmatically were called.
     */
    @Test
    public void noTimeout() throws Exception {
        StatefulBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertTrue("Synchronization before completion has to be called",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion has to be called",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources multiply two calls commits happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * Having two successful call to SFSB bean where two {@link XAResource}s are enlisted
     * and 2PC is processed. No timeout happens.
     * <p>
     * The other checks verifies that synchronization callback methods defined
     * by bean annotations were called.
     */
    @Test
    public void noTimeoutWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertEquals("Synchronization after begin has to be called", 2, checker.countSynchronizedBegin());
        Assert.assertEquals("Synchronization before completion has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources multiply two calls commits happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * Having two successful call to SFSB bean where two {@link XAResource}s are enlisted
     * and 2PC is processed. No timeout happens.
     * <p>
     * The other checks verifies that synchronization callback methods defined
     * by bean annotations and defined by fact that the bean implements {@link SessionSynchronization}
     * interface are called. There is important here that both types of callback were put into work.
     */
    @Test
    public void noTimeoutWithAnnotationAndInterface() throws Exception {
        StatefulWithAnnotationAndInterfaceBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationAndInterfaceBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertEquals("Synchronization after begin has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization before completion has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting one XA resource multiply two calls commits happened", 2, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * Having two successful call to SFSB bean where two {@link XAResource}s are enlisted
     * and 2PC is processed. No timeout happens.
     * <p>
     * The other checks verifies that synchronization callback methods defined
     * by using {@link TransactionSynchronizationRegistry} are invoked.
     */
    @Test
    public void noTimeoutWithRegistry() throws Exception {
        StatefulWithRegistryBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertTrue("Synchronization before completion has to be called",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion has to be called",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources multiply two calls commits happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing takes longer than transaction timeout is set
     * and that happens. Rollback is expected to be called on resources.
     * <p>
     * When rollback happens and exception is thrown from the bean then
     * container should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException} being thrown (WFLY-6212).
     */
    @Test
    public void timeout() throws Exception {
        StatefulBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollbacks happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException should discard the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing timeout happens and rollback of resources is expected.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back. See explanation of behavior at {@link Synchronization}.
     */
    @Test
    public void timeoutJtaSynchro() throws Exception {
        StatefulBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        try {
            bean.testTransactionTimeoutWithSynchronization();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException should discard the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * During the business method processing transaction is marked as rollback only.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back. See explanation of behavior at {@link Synchronization}.
     */
    @Test
    public void rollbackOnly() throws Exception {
        StatefulBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulBean.class);

        bean.testTransactionRollbackOnly();

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting one rollback happened on XA resource as TM was directed to rollback "
                + "just after first resource was enlisted", 2, checker.getRolledback());

        bean.touch();
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing timeout happens and rollback of resources is expected.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back based on the transaction timeout happens.
     * The synchronization callbacks are defined by bean annotations thus
     * see description of {@link BeforeCompletion} and {@link AfterCompletion}.
     */
    @Test
    public void timeoutWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException discarded the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing timeout happens and rollback of resources is expected.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back based on the transaction timeout happens.
     * The synchronization callbacks are defined by bean annotations (see {@link BeforeCompletion}
     * and {@link AfterCompletion}) and by adding synchronization on transaction directly
     * (see {@link Synchronization}.
     */
    @Test
    public void timeoutWithAnnotationAddingJtaSynchro() throws Exception {
        StatefulWithAnnotationBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        try {
            bean.testTransactionTimeoutSynchroAdded();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertEquals("Synchronization before completion should not be called as rollback happened",
                0, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion should be called twice",
                2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException discarded the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * During the business method processing transaction is marked as rollback only.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back.
     * Synchronization callbacks are defined by bean annotations thus
     * see description of {@link BeforeCompletion} and {@link AfterCompletion}.
     */
    @Test
    public void rollbackOnlyWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);

        bean.testTransactionRollbackOnly();

        Assert.assertFalse("Synchronization before completion should not be called as rollback happens",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happens",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting one rollback happened on XA resource as TM was directed to rollback "
                + "just after first resource was enlisted", 2, checker.getRolledback());

        bean.touch();
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing timeout happens and rollback of resources is expected.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back based based transaction timeout happens.
     * The synchronization callbacks are defined with injection of
     * {@link TransactionSynchronizationRegistry}.
     */
    @Test
    public void timeoutWithRegistry() throws Exception {
        StatefulWithRegistryBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException discarded the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    /**
     * Having a call to SFSB bean where two {@link XAResource}s are enlisted.
     * The bean call processing timeout happens and rollback of resources is expected.
     * <p>
     * Rollback and follow-up exception should discard the bean instance and on the next call
     * should be forbidden by {@link NoSuchEJBException}.
     * <p>
     * The other check verifies synchronization callback behavior on transaction
     * which is rolled-back based based transaction timeout happens.
     * The synchronization callbacks are defined with injection of
     * {@link TransactionSynchronizationRegistry} and at the same time with {@link Synchronization}.
     */
    @Test
    public void timeoutWithRegistryAddingJtaSynchro() throws Exception {
        StatefulWithRegistryBean bean = TransactionTestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        try {
            bean.testTransactionWithSynchronizationTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBTransactionRolledbackException.class, e.getClass());
        }

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            // RuntimeException discarded the SFSB
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by thrown EJBException");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }
}
