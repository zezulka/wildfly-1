package org.jboss.as.test.integration.transaction.synchronization;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import org.jboss.as.test.integration.transactions.TestSynchronization;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;

@Transactional
@ApplicationScoped
public class SynchronizationCdiBean {

    @Resource
    TransactionSynchronizationRegistry registry;

    @Inject
    private TransactionCheckerSingleton checker;

    public void commit() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        registry.registerInterposedSynchronization(sync);
    }

    public void rollback() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        registry.registerInterposedSynchronization(sync);
        throw new RuntimeException("cdi bean transaction rolled back");
    }

    public void rollbackOnly() throws Exception {
        TestSynchronization sync = new TestSynchronization(checker);
        registry.registerInterposedSynchronization(sync);
        registry.setRollbackOnly();
    }
}
