package org.jboss.as.test.integration.ejb.transaction.concurrency;

import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

@Singleton
// @ConcurrencyManagement(value = ConcurrencyManagementType.BEAN) - just trying to find out what happens here
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Remote(AsyncSingleton.class)
public class AsyncSingletonImpl implements AsyncSingleton {

    @Asynchronous
    public void doAsyncAction() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) {
        }
    }

}