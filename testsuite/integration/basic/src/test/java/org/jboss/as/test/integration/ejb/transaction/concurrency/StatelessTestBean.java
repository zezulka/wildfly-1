package org.jboss.as.test.integration.ejb.transaction.concurrency;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class StatelessTestBean {

    @EJB
    private AsyncSingleton asyncSingleton;

    public void call() throws Exception {
        try {
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
            asyncSingleton.doAsyncAction();
        }catch(Exception e) {
            throw e;
        }
    }
}