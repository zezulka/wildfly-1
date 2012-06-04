package org.jboss.as.test.integration.ejb.remote.contextselector;

import javax.ejb.Remote;

@Remote
public interface StatelessBeanRemote {
    String sayWhoHello();
}
