/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import com.arjuna.mw.wst11.UserTransaction;
import com.arjuna.mw.wst11.UserTransactionFactory;
import com.arjuna.wst.TransactionRolledBackException;
import com.arjuna.wst.WrongStateException;

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
    public void testWSATSimple() throws Exception {
        ut.begin();
        client.invoke();
        ut.commit();

        assertOrder(client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATSimpleMultiInvoke() throws Exception {
        ut.begin();
        client.invoke();
        client.invoke();
        ut.commit();

        assertOrder(client, BEFORE_PREPARE, PREPARE, COMMIT, VOLATILE_COMMIT);
    }

    @Test
    public void testWSATClientRollback() throws Exception {
        ut.begin();
        client.invoke();
        ut.rollback();

        //TODO: should rollback be called twice? once for volatile and once for durable - probinson
        assertOrder(client, ROLLBACK, VOLATILE_ROLLBACK);

    }

    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollback() throws Exception {
        try {
            ut.begin();
            client.invoke(VOTE_ROLLBACK);
            ut.commit();
        } catch (TransactionRolledBackException e) {
            assertOrder(client, BEFORE_PREPARE, PREPARE, VOLATILE_ROLLBACK);
            throw e;
        }
    }
    
    @Test(expected = TransactionRolledBackException.class)
    public void testWSATVoteRollbackPrePrepare() throws Exception {
        try {
            ut.begin();
            client.invoke(VOTE_ROLLBACK_PRE_PREPARE);
            ut.commit();
        } catch (TransactionRolledBackException e) {
            // TODO: is this ok assert order - where is the VOLATILE_ROLLBACK?
            assertOrder(client, BEFORE_PREPARE, ROLLBACK);
            throw e;
        }
    }
    
    @Test(expected = WrongStateException.class)
    public void testWSATRollbackOnly() throws Exception {
        try {
            ut.begin();
            client.invoke(ROLLBACK_ONLY);
            ut.commit();
        } catch (WrongStateException wse) {
            assertOrder(client, ROLLBACK, VOLATILE_ROLLBACK);
            throw wse;
        }
    }

    @Test
    public void testWSATVoteReadOnlyVolatile() throws Exception {
        ut.begin();
        client.invoke(VOTE_READONLY_VOLATILE); // volatile is bound to VOLATILE_COMMIT
        ut.commit();
    
        assertOrder(client, BEFORE_PREPARE, PREPARE, COMMIT);
    }
    
    @Test
    public void testWSATVoteReadOnlyDurable() throws Exception {
        ut.begin();
        client.invoke(VOTE_READONLY_DURABLE); // volatile is bound to COMMIT
        ut.commit();
    
        assertOrder(client, BEFORE_PREPARE, PREPARE, VOLATILE_COMMIT);
    }
    
    @Test
    public void testWSATVoteReadOnlyBothParticipants() throws Exception {
        ut.begin();
        client.invoke(VOTE_READONLY_DURABLE, VOTE_READONLY_VOLATILE);
        ut.commit();
    
        assertOrder(client, BEFORE_PREPARE, PREPARE);
    }

    @Test
    public void testWSATApplicationException() throws Exception {
        try {
            ut.begin();
            client.invoke(APPLICATION_EXCEPTION);
            Assert.fail("Exception should have been thrown by now");
        } catch (TestApplicationException e) {
            //Exception expected
        } finally {
            ut.rollback();
        }
        //TODO: should this cause Rollback? - probinson
        assertOrder(client, ROLLBACK, VOLATILE_ROLLBACK);
    }
}
