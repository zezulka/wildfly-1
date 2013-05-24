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

package org.jboss.as.test.xts.newxts.wsba.participantcompletion.client;

import com.arjuna.mw.wst11.UserBusinessActivity;
import com.arjuna.mw.wst11.UserBusinessActivityFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.xts.newxts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.wsba.participantcompletion.service.BAParticipantCompletion;
import org.jboss.jbossts.xts.bytemanSupport.BMScript;
import org.jboss.jbossts.xts.bytemanSupport.participantCompletion.ParticipantCompletionCoordinatorRules;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
                .addClass(ParticipantCompletionCoordinatorRules.class)

                .addAsResource("context-handlers.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
    }

    @BeforeClass()
    public static void submitBytemanScript() throws Exception {
        BMScript.submit(ParticipantCompletionCoordinatorRules.RESOURCE_PATH);
    }

    @AfterClass()
    public static void removeBytemanScript() {
        BMScript.remove(ParticipantCompletionCoordinatorRules.RESOURCE_PATH);
    }
    
    @Before
    public void setupTest() throws Exception {
        uba = UserBusinessActivityFactory.userBusinessActivity();
        client = BAParticipantCompletionClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        client.clearEventLog();
        client.clearData();
        cancelIfActive(uba);
    }

    @Test
    public void testWSBAParticipantCompleteSingle() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(1);

        uba.begin();
        client.saveData("single", DO_COMPLETE);
        uba.close();

        assertOrder("single", client, CONFIRM_COMPLETED, CLOSE);
    }
    
    @Test
    public void testWSBAParticipantComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        uba.begin();
        client.saveData("complete1", DO_COMPLETE);
        client.saveData("complete2", DO_COMPLETE);
        client.saveData("complete3", DO_COMPLETE);
        uba.close();

        assertOrder("complete1", client, CONFIRM_COMPLETED, CLOSE);
        assertOrder("complete2", client, CONFIRM_COMPLETED, CLOSE);
        assertOrder("complete3", client, CONFIRM_COMPLETED, CLOSE);
    }
    
    @Test
    public void testWSBAParticipantDoNotComplete() throws Exception {
        ParticipantCompletionCoordinatorRules.setParticipantCount(3);

        try {
            uba.begin();
            client.saveData("notcomplete1", DO_COMPLETE);
            client.saveData("notcomplete2", DO_COMPLETE);
            client.saveData("notcomplete3");  // this participant does not inform about correct completition
            uba.close();
            
            Assert.fail("Exception should have been thrown by now");
        } catch(TransactionRolledBackException e) {
            // we expect this :)
        }

        assertOrder("notcomplete1", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("notcomplete2", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("notcomplete3", client, CANCEL);
    }

    @Test
    public void testWSBAParticipantClientCancel() throws Exception {
        uba.begin();
        client.saveData("participantclientcancel1", DO_COMPLETE);
        client.saveData("participantclientcancel2", DO_COMPLETE);
        client.saveData("participantclientcancel3", DO_COMPLETE);
        uba.cancel();

        assertOrder("participantclientcancel1", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("participantclientcancel2", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("participantclientcancel3", client, CONFIRM_COMPLETED, COMPENSATE);
    }
    
    @Test
    public void testWSBAParticipantClientCancelNotComplete() throws Exception {
        uba.begin();
        client.saveData("participantclientcancel1", DO_COMPLETE);
        client.saveData("participantclientcancel2");
        client.saveData("participantclientcancel3", DO_COMPLETE);
        uba.cancel();

        assertOrder("participantclientcancel1", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("participantclientcancel2", client, CANCEL);
        assertOrder("participantclientcancel3", client, CONFIRM_COMPLETED, COMPENSATE);
    }

    @Test
    public void testWSBAParticipantApplicationException() throws Exception {
        try {
            uba.begin();
            client.saveData("participantappexception1", DO_COMPLETE);
            client.saveData("participantappexception2", DO_COMPLETE);
            client.saveData("participantappexception3", APPLICATION_EXCEPTION);
            
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            // Exception is expected
        } finally {
            uba.cancel();
        }
        
        assertOrder("participantappexception1", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("participantappexception2", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("participantappexception3", client, CANCEL);
    }

    @Test
    public void testWSBAParticipantCannotComplete() throws Exception {
        try {
            uba.begin();
            client.saveData("cannotcomplete1", DO_COMPLETE);
            client.saveData("cannotcomplete2", CANNOT_COMPLETE);
            client.saveData("cannotcomplete3", DO_COMPLETE);
            
            Assert.fail("Exception should have been thrown by now");
        } catch (javax.xml.ws.soap.SOAPFaultException sfe) {
            // Exception is expected - enlisting participant #3 can't be done
        }
        
        try {
            uba.close();
        } catch(TransactionRolledBackException e) {
            // Exception is expected - rollback on close because of cannotComplete
        }
        
        // TODO: CANCEL here?
        assertOrder("cannotcomplete1", client, CONFIRM_COMPLETED, COMPENSATE);
        assertOrder("cannotcomplete2", client);
        assertOrder("cannotcomplete3", client);
    }
}
