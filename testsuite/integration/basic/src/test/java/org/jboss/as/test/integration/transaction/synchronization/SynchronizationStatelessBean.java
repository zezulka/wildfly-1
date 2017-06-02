/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.integration.transaction.synchronization;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;
import org.jboss.as.test.integration.transactions.TestSynchronization;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;

/**
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Stateless
public class SynchronizationStatelessBean {
    @EJB
    private TransactionCheckerSingletonRemote checker;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager tm;

    public void commit() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        tm.getTransaction().registerSynchronization(sync);
    }

    public void rollback() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        tm.getTransaction().registerSynchronization(sync);
        throw new RuntimeException("rolling back the CMT");
    }

    public void rollbackOnly() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        tm.getTransaction().registerSynchronization(sync);
        tm.setRollbackOnly();
    }
}
