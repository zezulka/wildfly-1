package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;

/**
 * @author Ondrej Chaloupka
 */
public class AsyncBeanParent {
    public void asyncMethod() throws InterruptedException {
    }
    
    public Future<Boolean> futureMethod() throws InterruptedException, ExecutionException {
        return new AsyncResult<Boolean>(true);
    }  
}