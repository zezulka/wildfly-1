/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.weld.jta.observe;

import java.util.List;

import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * <p>
 * Testcase which checks how the CDI observer works
 * with the transactional phases (see {@link TransactionPhase}).
 * <p>
 * This test is reproducer for <a href="https://issues.jboss.org/browse/WELD-2444">WELD-2444</a>.
 */
@RunWith(Arquillian.class)
public class TransactionalObserverJPAListenerTestCase {
    private static final Logger log = Logger.getLogger(TransactionalObserverJPAListenerTestCase.class);
    private static final String ARCHIVE_NAME = "transactional-event-observer-jpa-event";

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar")
            .addPackage(TransactionalObserverJPAListenerTestCase.class.getPackage())
            .addAsManifestResource(TransactionalObserverJPAListenerTestCase.class.getPackage(), "persistence.xml", "persistence.xml")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ArquillianResource
    private static InitialContext iniCtx;

    @Inject
    TestEntityManipulatorBean bean;

    @Before
    public void setUp() {
        TransactionalObserverBean.clearFlags();
    }

    @Test
    public void commitObserved() {
        int beforeCount = bean.getAllEntities().size();

        log.info("Before entity creation...");
        bean.createTestEntity("commit entity");
        log.info("After entity creation...");

        List<TestEntity> afterTestEntities = bean.getAllEntities();
        Assert.assertEquals("Expecting one more entity was created but there is no more entities than at start "
            + " count of entities in db at start: " + beforeCount + ", entities now: " + afterTestEntities,
            beforeCount + 1, afterTestEntities.size());
        Assert.assertFalse("As the transaction was committed the failure should not be observed",
                TransactionalObserverBean.isTransactionFailureObserved);
        Assert.assertTrue("As the transaction was committed the success should be observed",
                TransactionalObserverBean.isTransactionSuccessObserved);
    }

    // @Test ?
    public void rollbackObserved() {
        int beforeCount = bean.getAllEntities().size();

        try {
            log.info("Before entity creation...");
            bean.createTestEntityFailure("rollback entity");
        } catch (RuntimeException expected) {
            // ignored as expected runtime exception to get rollback
        }
        log.info("After entity creation...");

        List<TestEntity> afterTestEntities = bean.getAllEntities();
        Assert.assertEquals("Expecting noentity was created or deleted but the count does not match what "
            + "was in db at start: " + beforeCount + ", entities now: " + afterTestEntities,
            beforeCount, afterTestEntities.size());
        Assert.assertTrue("As the transaction was rolled-back the failure should be observed",
                TransactionalObserverBean.isTransactionFailureObserved);
        Assert.assertFalse("As the transaction was rolled-back the success should not be observed",
                TransactionalObserverBean.isTransactionSuccessObserved);
    }

}
