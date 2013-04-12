/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.xts.newxts.wsba.coordinatorcompletition.client;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.newxts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.wsba.coordinatorcompletition.service.BACoordinatorCompletion;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.xts.newxts.util.ServiceCommand.*;
import static org.jboss.as.test.xts.newxts.util.EventLogEvent.*;

/**
 * XTS business activities - coordinator completition test case
 */
@RunWith(Arquillian.class)
public class BACoordinatorCompletionTestCase extends BaseFunctionalTest {
    UserBusinessActivity uba;
    BACoordinatorCompletion client;

    public static final String ARCHIVE_NAME = "wsba-coordinatorcompletition-test";

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addPackage(BACoordinatorCompletion.class.getPackage())
                .addPackage(BACoordinatorCompletionClient.class.getPackage())
                .addPackage(EventLog.class.getPackage())
                .addPackage(BaseFunctionalTest.class.getPackage())

                // .addAsManifestResource("persistence.xml")
                .addAsResource("context-handlers.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Before
    public void setupTest() throws Exception {
        uba = UserBusinessActivityFactory.userBusinessActivity();
        client = BACoordinatorCompletionClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        assertDataAvailable(client);
        client.clearEventLog();
        cancelIfActive(uba);
    }

    @Test
    public void testWSBACoordinatorSimple() throws Exception {
        uba.begin();
        client.saveData("test");
        uba.close();

        assertOrder(client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBACoordinatorMultiInvoke() throws Exception {
        uba.begin();
        client.saveData("test1");
        client.saveData("test2");
        uba.close();

        assertOrder(client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBACoordinatorClientCancel() throws Exception {
        uba.begin();
        client.saveData("test");
        uba.cancel();

        assertOrder(client, CANCEL);
    }

    @Test
    public void testWSBACoordinatorApplicationException() throws Exception {
        try {
            uba.begin();
            client.saveData("test", APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            //TODO: is the test app exception ok? - don't we expect SOAPFaultException
            // This is OK - exception expected
        } finally {
            uba.cancel();
        }
        // TODO: called cancel which seems to be OK, is it?
        assertOrder(client, CANCEL);
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSBACoordinatorCannotComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("test", CANNOT_COMPLETE);
            uba.close();
        } catch (TransactionRolledBackException e) {
            assertOrder(client);
            throw e;
        }
    }
    
    @Test
    public void testWSBACoordinatorDoComplete() throws Exception {
        uba.begin();
        client.saveData("test", DO_COMPLETE);
        uba.close();

        // TODO: there is confirmCompleted called with parameter "false" so rollback is called
        //       at least in our participant - isn't that strange?
        assertOrder(client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSBACoordinatorSystemExceptionOnComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("test", SYSTEM_EXCEPTION_ON_COMPLETE);
            uba.close();
        } catch (TransactionRolledBackException e) {
            assertOrder(client, COMPLETE, CANCEL);   
            throw e;
        }
    }
}
