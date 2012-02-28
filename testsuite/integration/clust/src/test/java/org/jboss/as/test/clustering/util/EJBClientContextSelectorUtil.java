package org.jboss.as.test.clustering.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.PropertiesBasedEJBClientConfiguration;
import org.jboss.ejb.client.remoting.ConfigBasedEJBClientContextSelector;
import org.jboss.logging.Logger;

public class EJBClientContextSelectorUtil {
    private static Logger log = Logger.getLogger(EJBClientContextSelectorUtil.class);

    /**
     * Sets up EJB client context. @see {EJBClientContextSelectorUtil.setupEJBClientContextSelector}
     * @return previous ContextSelector
     * @throws IOException
     */
    public ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        return this.setupEJBClientContextSelector(null);
    }
    
    /**
     * Sets up the EJB client context to use a selector which processes and sets up EJB receivers
     * based on this testcase specific jboss-ejb-client.properties file
     *
     * @param properties which will be merged with properties from file
     * @return previous ContextSelector
     * @throws java.io.IOException
     */
    public ContextSelector<EJBClientContext> setupEJBClientContextSelector(Properties propertiesToMerge) throws IOException {       
        // setup the selector
        final String clientPropertiesFile = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
        final InputStream inputStream = EJBClientContextSelectorUtil.class.getClassLoader().getResourceAsStream(clientPropertiesFile);
        if (inputStream == null) {
            throw new IllegalStateException("Could not find " + clientPropertiesFile + " in classpath");
        }
        final Properties properties = new Properties();
        properties.load(inputStream);
        
        
        // Merging properties from method argument
        if(propertiesToMerge == null) {
            propertiesToMerge = new Properties();
        }
        for(Object key: propertiesToMerge.keySet()) {
            properties.put(key, propertiesToMerge.get(key));
            log.debug("Adding/replacing property: " + key + " => " + propertiesToMerge.get(key));
        }
        
        final EJBClientConfiguration ejbClientConfiguration = new PropertiesBasedEJBClientConfiguration(properties);
        final ConfigBasedEJBClientContextSelector selector = new ConfigBasedEJBClientContextSelector(ejbClientConfiguration);
        log.debug("Setting new context selector " + selector);
        return EJBClientContext.setSelector(selector);
    }
    
}
