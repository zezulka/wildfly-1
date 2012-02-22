package org.jboss.as.test.clustering.cluster.localcall;

import javax.ejb.Remote;

/**
 * @author Ondrej Chaloupka
 */
@Remote
public interface SynchronizationSingletonInterface {
    public void resetLatches();

    void countDownLatchNumber1();

    void countDownLatchNumber2();

    void waitForLatchNumber1() throws InterruptedException;

    void waitForLatchNumber2() throws InterruptedException;
}
