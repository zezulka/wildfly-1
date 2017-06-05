package org.jboss.as.test.integration.weld.jta;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

@ApplicationScoped
public class CdiBean {

    // @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    // @Resource(lookup = "java:jboss/TransactionSynchronizationRegistry")
    // @Resource(lookup = "java:/TransactionSynchronizationRegistry")
    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager transactionManager;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
    }

    public boolean isTransactionManagerInjected() {
        return transactionManager != null;
    }
}
