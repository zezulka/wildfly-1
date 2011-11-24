package org.jboss.as.test.integration.ejb.interceptor.complex.a;

import javax.ejb.Remote;

@Remote
public interface MyTestRemoteB {
    boolean doit();
}
