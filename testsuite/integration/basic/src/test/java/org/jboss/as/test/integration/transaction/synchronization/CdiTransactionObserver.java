package org.jboss.as.test.integration.transaction.synchronization;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;

import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CdiTransactionObserver {
    private static final Logger log = Logger.getLogger(CdiTransactionObserver.class);

    @Inject
    private TransactionCheckerSingleton checker;

    public void observesTransactionProgress(@Observes(during = TransactionPhase.IN_PROGRESS) TransactionEventData TransactionEventData) {
        log.trace("Transaction in progress");
    }

    public void observesBeforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) TransactionEventData TransactionEventData) {
        log.trace("Transaction before completion");
        checker.setSynchronizedBefore();
    }

    public void observesAfterCompletion(@Observes(during = TransactionPhase.AFTER_COMPLETION) TransactionEventData TransactionEventData) {
        log.trace("Transaction after completion");
        checker.setSynchronizedAfter(true);
    }

    public void observesAfterSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) TransactionEventData TransactionEventData) {
        log.trace("Transaction on commit");
        checker.addCommit();
    }

    public void observesAfterFailure(@Observes(during = TransactionPhase.AFTER_FAILURE) TransactionEventData TransactionEventData) {
        log.trace("Transaction on rollback");
        checker.addRollback();
    }

}