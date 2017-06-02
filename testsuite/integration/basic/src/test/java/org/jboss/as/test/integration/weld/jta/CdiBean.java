package org.jboss.as.test.integration.weld.jta;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

@ApplicationScoped
public class CdiBean {

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    // @Resource(lookup = "java:/TransactionManager")
    // TransactionManager txnMgr;

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager transactionManager;

    public boolean isTransactionSynchronizationRegistryInjected() {
        return transactionSynchronizationRegistry != null;
        // return txnMgr != null;
    }

    public boolean isTransactionManagerInjected() {
        return transactionManager != null;
    }

}
