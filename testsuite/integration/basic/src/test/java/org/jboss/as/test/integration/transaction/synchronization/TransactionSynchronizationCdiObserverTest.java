package org.jboss.as.test.integration.transaction.synchronization;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.transactions.TestSynchronization;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TransactionSynchronizationCdiObserverTest {
    @EJB
    private TransactionCheckerSingletonRemote checker;

    @Inject
    private SynchronizationCdiObserverBean cdiBean;

    @Before
    public void setUp() {
        checker.resetAll();
    }

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackage(TestSynchronization.class.getPackage())
                .addClasses(TransactionSynchronizationCdiObserverTest.class, SynchronizationCdiObserverBean.class,
                        TransactionEventData.class, CdiTransactionObserver.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void commit() throws Exception {
        cdiBean.commit();
        Assert.assertTrue("CMT was committed Synchronization.beforeCompletion is expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was committed Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }

    @Test
    public void rollback() throws Exception {
        try {
            cdiBean.rollback();
        } catch (RuntimeException expected) {
            // expected, txn rolled  back by throwning runtime exception
        }
        Assert.assertFalse("CMT was rolled back Synchronization.beforeCompletion is not expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was committed Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }

    @Test
    public void rollbackOnly() throws Exception {
        cdiBean.rollbackOnly();
        Assert.assertFalse("CMT was set as roll-only Synchronization.beforeCompletion is not expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was set as roll-only Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }
}
