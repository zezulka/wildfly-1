package org.jboss.as.test.integration.transaction.synchronization;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.Transactional;
import org.jboss.as.test.integration.transactions.TestSynchronization;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.logging.Logger;

@Transactional
@ApplicationScoped
public class SynchronizationCdiBean {
    private static final Logger log = Logger.getLogger(SynchronizationCdiBean.class);

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
        log.trace(">>>>>>>>>>>>>>>>>>>>>> Before synchronization registered");
        registry.registerInterposedSynchronization(sync);
        log.trace(">>>>>>>>>>>>>>>>>>>>>> Before rollback only");
        registry.setRollbackOnly();
        log.trace(">>>>>>>>>>>>>>>>>>>>>> After rollback only");
    }
}
