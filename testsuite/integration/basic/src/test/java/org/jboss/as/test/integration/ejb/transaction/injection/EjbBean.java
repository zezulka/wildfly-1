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

package org.jboss.as.test.integration.ejb.transaction.injection;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

@Stateless
public class EjbBean {

    @Resource
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager txnManager;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
    }

    public boolean isTransactionManagerInjected() {
        return txnManager != null;
    }
}
