package org.jboss.as.test.integration.weld.jta;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionSynchronizationRegistry;

@Stateless
public class EjbBean {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
    }
}
