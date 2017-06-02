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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
public class TransactionSynchronizationStsbTest {

    @EJB
    private TransactionCheckerSingletonRemote checker;

    @EJB
    private SynchronizationStatelessBean statelessBean;

    @Inject
    private SynchronizationCdiBean cdiBean;

    @Before
    public void setUp() {
        checker.resetAll();
    }

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "transaction-synchronization-stsb.jar")
            .addClasses(TransactionSynchronizationStsbTest.class, SynchronizationStatelessBean.class)
            .addPackage(TestSynchronization.class.getPackage())
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void commit() throws Exception {
        statelessBean.commit();
        Assert.assertTrue("CMT was committed Synchronization.beforeCompletion is expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was committed Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }

    @Test
    public void rollback() throws Exception {
        try {
            statelessBean.rollback();
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
        statelessBean.rollbackOnly();
        Assert.assertFalse("CMT was set as roll-only Synchronization.beforeCompletion is not expected to be called",
            checker.isSynchronizedBefore());
        Assert.assertTrue("CMT was set as roll-only Synchronization.afterCompletion is expected to be called",
            checker.isSynchronizedAfter());
    }
}
