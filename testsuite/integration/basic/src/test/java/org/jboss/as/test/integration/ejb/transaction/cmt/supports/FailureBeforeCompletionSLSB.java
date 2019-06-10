/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.transaction.cmt.supports;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

@Stateless
public class FailureBeforeCompletionSLSB {
    private static final Logger log = Logger.getLogger(FailureBeforeCompletionSLSB.class);

    // @Resource(mappedName = "java:comp/TransactionSynchronizationRegistry")
    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String doStuff() {
        transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {

            @Override
            public void beforeCompletion() {
                log.info("Before completion called");
                throw new javax.persistence.PersistenceException("intentional exception for testing");
            }

            @Override
            public void afterCompletion(int status) {
                log.info("After completion called with status " + status);
            }
        });
        return "work done";
    }
}
