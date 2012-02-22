package org.jboss.as.test.clustering.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.as.test.clustering.unmanaged.ejb3.stateful.remote.failover.dd.RemoteEJBClientDDBasedSFSBFailoverTestCase;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;

public class EJBClientContextSelectorUtil {

    /**
     * Sets up the EJB client context to use a selector which processes and sets up EJB receivers
     * based on this testcase specific jboss-ejb-client.properties file
     *
     * @return
     * @throws java.io.IOException
     */
    public ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        // setup the selector
        final String clientPropertiesFile = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
        final InputStream inputStream = RemoteEJBClientDDBasedSFSBFailoverTestCase.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);

        return EJBClientContext.setSelector(selector);
    }
    
}
