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

package org.jboss.as.test.xts.newxts.wsba.coordinatorcompletion.client;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.newxts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.wsba.coordinatorcompletion.service.BACoordinatorCompletion;
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
        client.clearEventLog();
        cancelIfActive(uba);
    }

    @Test
    public void testWSBACoordinatorSingle() throws Exception {
        uba.begin();
        client.saveData("single");
        uba.close();

        assertOrder("single", client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBACoordinatorSimple() throws Exception {
        uba.begin();
        client.saveData("simple1");
        client.saveData("simple2");
        client.saveData("simple3");
        uba.close();

        assertOrder("simple1", client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
        assertOrder("simple2", client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
        assertOrder("simple3", client, COMPLETE, CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBACoordinatorClientCancel() throws Exception {
        uba.begin();
        client.saveData("clientcancel1");
        client.saveData("clientcancel2");
        client.saveData("clientcancel3");
        uba.cancel();

        assertOrder("clientcancel1", client, CANCEL);
        assertOrder("clientcancel2", client, CANCEL);
        assertOrder("clientcancel3", client, CANCEL);
    }

    @Test
    public void testWSBACoordinatorApplicationException() throws Exception {
        try {
            uba.begin();
            client.saveData("applicationexception1");
            client.saveData("applicationexception2");
            client.saveData("applicationexception3", APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            // This is OK - exception expected
        } finally {
            uba.cancel();
        }

        assertOrder("applicationexception1", client, CANCEL);
        assertOrder("applicationexception2", client, CANCEL);
        assertOrder("applicationexception3", client, CANCEL);
    }

    @Test
    public void testWSBACoordinatorCannotComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("coordinatorcannotcomplete1");
            client.saveData("coordinatorcannotcomplete2", CANNOT_COMPLETE);
            client.saveData("coordinatorcannotcomplete3");
            uba.close();
            
            Assert.fail("Exception should have been thrown by now");
        } catch (javax.xml.ws.soap.SOAPFaultException sfe) {
            assertOrder("coordinatorcannotcomplete1", client);
            assertOrder("coordinatorcannotcomplete2", client);
            assertOrder("coordinatorcannotcomplete3", client);
        }
    }

    @Test
    public void testWSBACoordinatorSystemExceptionOnComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("systemexceptiononcomplete1");
            client.saveData("systemexceptiononcomplete2", SYSTEM_EXCEPTION_ON_COMPLETE);
            client.saveData("systemexceptiononcomplete3");
            uba.close();
            
            Assert.fail("Exception should have been thrown by now");
        } catch (com.arjuna.wst.TransactionRolledBackException trbe) {
            assertOrder("systemexceptiononcomplete1", client, COMPLETE, CONFIRM_COMPLETED, COMPENSATE);
            assertOrder("systemexceptiononcomplete2", client, COMPLETE, CANCEL);
            assertOrder("systemexceptiononcomplete3", client, CANCEL);
        }
    }
}
