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

package org.jboss.as.test.xts.newxts.wsat.client;

import javax.xml.ws.soap.SOAPFaultException;

import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import com.arjuna.wst.TransactionRolledBackException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;

import org.jboss.as.test.xts.newxts.base.BaseFunctionalTest;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.wsat.service.AT;

import static org.jboss.as.test.xts.newxts.util.ServiceCommand.*;
import static org.jboss.as.test.xts.newxts.util.EventLogEvent.*;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * XTS atomic transaction test case
 */
@RunWith(Arquillian.class)
public class ATTestCase extends BaseFunctionalTest {

    private UserTransaction ut;
    private AT client;
    
    public static final String ARCHIVE_NAME = "wsat-test";

    
    @Deployment
    public static WebArchive createTestArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war")
                .addPackage(AT.class.getPackage())
                .addPackage(ATClient.class.getPackage())
                .addPackage(EventLog.class.getPackage())
                .addPackage(BaseFunctionalTest.class.getPackage())
                
                .addAsResource("context-handlers.xml") // is this needed?
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.xts,org.jboss.jts\n"), "MANIFEST.MF");
 
        return archive;
    }

    @Before
    public void setupTest() throws Exception {
        ut = UserTransactionFactory.userTransaction();
        client = ATClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        client.clearEventLog();
        rollbackIfActive(ut);
    }

    @Test
    public void testWSATSingleSimple() throws Exception {
        ut.begin();
        client.invoke("single");
        ut.commit();

        assertOrder("single", client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATSimple() throws Exception {
        ut.begin();
        client.invoke("simple1");
        client.invoke("simple2");
        client.invoke("simple3");
        ut.commit();

        assertOrder("simple1", client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertOrder("simple2", client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
        assertOrder("simple3", client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATClientRollback() throws Exception {
        ut.begin();
        client.invoke("clientrollback1");
        client.invoke("clientrollback2");
        client.invoke("clientrollback3");
        ut.rollback();

        assertOrder("clientrollback1", client, ROLLBACK, VOLATILE_ROLLBACK);
        assertOrder("clientrollback2", client, ROLLBACK, VOLATILE_ROLLBACK);
        assertOrder("clientrollback2", client, ROLLBACK, VOLATILE_ROLLBACK);
    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollback() throws Exception {
        try {
            ut.begin();
            client.invoke("voterollback1");
            client.invoke("voterollback2", VOTE_ROLLBACK); // rollback voted on durable participant
            client.invoke("voterollback3");
            ut.commit();
        } catch (TransactionRolledBackException e) {
            assertOrder("voterollback1", client, BEFORE_PREPARE, PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            assertOrder("voterollback2", client, BEFORE_PREPARE, PREPARE, VOLATILE_ROLLBACK);
            assertOrder("voterollback3", client, BEFORE_PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            throw e;
        }
    }
    
    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollbackPrePrepare() throws Exception {
        try {
            ut.begin();
            client.invoke("voterollbackpreprepare1");
            client.invoke("voterollbackpreprepare2", VOTE_ROLLBACK_PRE_PREPARE); // rollback voted on volatile participant
            client.invoke("voterollbackpreprepare3");
            ut.commit();
        } catch (TransactionRolledBackException e) {
            // TODO: all different from spreadsheet
            assertOrder("voterollbackpreprepare1", client, BEFORE_PREPARE, ROLLBACK, VOLATILE_ROLLBACK);
            assertOrder("voterollbackpreprepare2", client, BEFORE_PREPARE, ROLLBACK);
            assertOrder("voterollbackpreprepare3", client, ROLLBACK, VOLATILE_ROLLBACK);
            throw e;
        }
    }

    @Test
    public void testWSATRollbackOnly() throws Exception {
        try {
            ut.begin();
            client.invoke("rollbackonly1");
            client.invoke("rollbackonly2", ROLLBACK_ONLY);
            client.invoke("rollbackonly3"); // failing on enlisting next participant
            // ut.commit();
            Assert.fail("The " + SOAPFaultException.class.getName() + " is expected for RollbackOnly test");
        } catch (SOAPFaultException sfe) {
            assertOrder("rollbackonly1", client, ROLLBACK, VOLATILE_ROLLBACK);
            assertOrder("rollbackonly2", client, ROLLBACK, VOLATILE_ROLLBACK);
            assertOrder("rollbackonly3", client);
        }
    }

    @Test
    public void testWSATVoteReadOnly() throws Exception {
        ut.begin();
        client.invoke("readonly1", VOTE_READONLY_VOLATILE); // volatile for VOLATILE_COMMIT
        client.invoke("readonly2", VOTE_READONLY_DURABLE); // durable for COMMIT
        client.invoke("readonly3", VOTE_READONLY_DURABLE, VOTE_READONLY_VOLATILE);
        ut.commit();
    
        assertOrder("readonly1", client, BEFORE_PREPARE, PREPARE, COMMIT);
        assertOrder("readonly2", client, BEFORE_PREPARE, PREPARE, VOLATILE_COMMIT);
        assertOrder("readonly3", client, BEFORE_PREPARE, PREPARE);
    }

    @Test
    public void testWSATApplicationException() throws Exception {
        try {
            ut.begin();
            client.invoke("applicationexception1");
            client.invoke("applicationexception2");
            client.invoke("applicationexception3", APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            //Exception expected
        } finally {
            ut.rollback();
        }

        assertOrder("applicationexception1", client, ROLLBACK, VOLATILE_ROLLBACK);
        assertOrder("applicationexception2", client, ROLLBACK, VOLATILE_ROLLBACK);
        assertOrder("applicationexception3", client, ROLLBACK, VOLATILE_ROLLBACK);
    }
}
