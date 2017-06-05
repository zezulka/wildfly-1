package org.jboss.as.test.integration.transaction.synchronization;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;

@Transactional
@ApplicationScoped
public class SynchronizationCdiObserverBean {

    @Resource(lookup = "java:/TransactionManager")
    TransactionManager txn;

    @Inject @Any Event<TransactionEventData> txnEvent;

    public void commit() throws Exception {
        txnEvent.fire(new TransactionEventData());
    }

    public void rollback() throws Exception {
        txnEvent.fire(new TransactionEventData());
        throw new RuntimeException("cdi bean transaction rolled back");
    }

    public void rollbackOnly() throws Exception {
        txn.setRollbackOnly();
        txnEvent.fire(new TransactionEventData());
    }
}
