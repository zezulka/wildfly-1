/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.integration.transaction.synchronization;

import javax.ejb.EJB;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.transactions.TestSynchronization;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for transaction synchronization callbacks.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class TransactionSynchronizationCdiTest {
    private static final Logger log = Logger.getLogger(TransactionSynchronizationCdiTest.class);

    @EJB
    private TransactionCheckerSingletonRemote checker;

    @Inject
    private SynchronizationCdiBean cdiBean;

    @Before
    public void setUp() {
        log.tracef("calling checker %s to reset counters on @Before", checker);
        checker.resetAll();
        log.tracef("checker %s call to reset counters returned to test execution", checker);
    }

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackage(TestSynchronization.class.getPackage())
                .addClasses(TransactionSynchronizationCdiTest.class, SynchronizationCdiBean.class)
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
            // expected as roll back is caused by throwning runtime exception
        }
        Assert.assertFalse("CMT was rolled back Synchronization.beforeCompletion is not expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was committed Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }

    @Test
    public void rollbackOnly() throws Exception {
        log.tracef("before calling cdi bean ", cdiBean);
        cdiBean.rollbackOnly();
        log.tracef("after the bean %s was called and returned the excecution back to test", cdiBean);

        log.tracef("calling checker %s to see before synchronized counter for bean %s", checker, cdiBean);
        Assert.assertFalse("CMT was set as roll-only Synchronization.beforeCompletion is not expected to be called",
            checker.isSynchronizedBefore());
        log.tracef("checker %s called to get before synchronized counter for bean %s", checker, cdiBean);
        log.tracef("calling checker %s to see after synchronized counter for bean %s", checker, cdiBean);
        Assert.assertTrue("CMT was set as roll-only Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
        log.tracef("checker %s called to get after synchronized counter for bean %s", checker, cdiBean);
    }
}
