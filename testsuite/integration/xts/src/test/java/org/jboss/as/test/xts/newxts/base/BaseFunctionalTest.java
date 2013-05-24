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

package org.jboss.as.test.xts.newxts.base;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.test.xts.newxts.util.EventLogEvent;
import org.junit.Assert;

import com.arjuna.mw.wst11.UserBusinessActivity;

/**
 * Shared functionality with XTS test cases. 
 */
public class BaseFunctionalTest {
    
    // ---- Work with transactions
    public void rollbackIfActive(com.arjuna.mw.wst11.UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }
    
    public void rollbackIfActive(javax.transaction.UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }

    public void cancelIfActive(UserBusinessActivity uba) {
        try {
            uba.cancel();
        } catch (Throwable e) {
            // do nothing, not active
        }
    }
    
    
    // ---- Test result checking
    protected void assertOrder(String eventLogName, BaseServiceInterface client, EventLogEvent... expectedOrder) {
        Assert.assertEquals(Arrays.asList(expectedOrder), client.getEventLog().getEventLog(eventLogName));
    }
    
    protected void assertDataAvailable(BaseServiceInterface client)
    {
        List<EventLogEvent> log = client.getEventLog().getDataUnavailableLog();
        if (!log.isEmpty())
        {
            org.junit.Assert.fail("One or more lifecycle methods could not access the managed data: " + log.toString());
        }
    }
}
