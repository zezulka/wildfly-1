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

package org.jboss.as.test.xts.newxts.wsba.participantcompletition.client;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.newxts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.util.ParticipantCompletionCoordinatorRules;
import org.jboss.as.test.xts.newxts.wsba.participantcompletition.service.BAParticipantCompletion;
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
 * XTS business activities - participant completition test case
 */
@RunWith(Arquillian.class)
public class BAParticipantCompletionTestCase extends BaseFunctionalTest {
    UserBusinessActivity uba;
    BAParticipantCompletion client;

    public static final String ARCHIVE_NAME = "wsba-participantcompletition-test";

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addPackage(BAParticipantCompletion.class.getPackage())
                .addPackage(BAParticipantCompletionClient.class.getPackage())
                .addPackage(EventLog.class.getPackage())
                .addPackage(BaseFunctionalTest.class.getPackage())

                .addAsResource("context-handlers.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @Before
    public void setupTest() throws Exception {
        uba = UserBusinessActivityFactory.userBusinessActivity();
        client = BAParticipantCompletionClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        assertDataAvailable(client);
        client.clearEventLog();
        client.clearData();
        cancelIfActive(uba);
    }

    @Test
    public void testWSBAParticipantComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(1);

        uba.begin();
        client.saveData("test", DO_COMPLETE);
        uba.close();

        assertOrder(client, CONFIRM_COMPLETED, CLOSE);
    }

    @Test
    public void testWSBAParticipantCompleteClose() throws Exception {
        try {
            uba.begin();
            client.saveData("test");  // no completition here!
            uba.close();
            Assert.fail("Exception should have been thrown by now");
        } catch (TransactionRolledBackException e) {
            // TODO: why no COMPENSATE is called?
            assertOrder(client);
        }
    }

    
    @Test
    public void testWSBAParticipantMultiInvoke() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(1);

        uba.begin();
        client.saveData("test1");
        client.saveData("test2", DO_COMPLETE);
        uba.close();

        assertOrder(client, CONFIRM_COMPLETED, CLOSE);
    }
    
    @Test
    public void testWSBAParticipantClientCancel() throws Exception {
        uba.begin();
        client.saveData("test", DO_COMPLETE);
        uba.cancel();

        assertOrder(client, CONFIRM_COMPLETED, COMPENSATE);
    }

    @Test
    public void testWSBAParticipantApplicationException() throws Exception {
        try {
            uba.begin();
            client.saveData("test", APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            // TODO: is the test app exception ok? - don't we expect SOAPFaultException
            // This is OK - exception expected
        } finally {
            uba.cancel();
        }
        assertOrder(client, CANCEL);
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSBAParticipantCannotComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("test", CANNOT_COMPLETE);
            uba.close();
        } catch (TransactionRolledBackException e) {
            // TODO: there is no cancel called - is this OK?
            assertOrder(client);
            throw e;
        }
    }
}
