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

import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;

import org.jboss.logging.Logger;

public class TransactionalObserverBean {
    private static final Logger log = Logger.getLogger(TransactionalObserverBean.class);

    static boolean isTransactionSuccessObserved, isTransactionFailureObserved;

    static void clearFlags() {
        TransactionalObserverBean.isTransactionSuccessObserved = false;
        TransactionalObserverBean.isTransactionFailureObserved = false;
    }

    public void postPersistObserverTransactionSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostPersistEvent postPersistEvent) {
        log.info("observingTransactionSuccess was invoked");
        TransactionalObserverBean.isTransactionSuccessObserved = true;
    }

    public void postPersistObserverTransactionFailure(@Observes(during = TransactionPhase.AFTER_FAILURE) PostPersistEvent postPersistEvent) {
        log.info("observingTransactionFailure was invoked");
        TransactionalObserverBean.isTransactionFailureObserved = true;
    }
}
