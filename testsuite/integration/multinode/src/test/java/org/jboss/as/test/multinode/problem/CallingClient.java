package org.jboss.as.test.multinode.problem;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class CallingClient {
    @EJB
    private StatefulLocal bean;
    
    public int call() throws Exception {
        return bean.call();
    }
}
