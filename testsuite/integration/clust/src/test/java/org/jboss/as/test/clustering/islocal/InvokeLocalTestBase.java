/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.islocal;

import java.io.IOException;
import java.rmi.dgc.VMID;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * @author Brian Stansberry
 */
public abstract class InvokeLocalTestBase {
    private static final Logger log = Logger.getLogger(InvokeLocalTestBase.class);
    protected static ContextSelector<EJBClientContext> previousSelector;
    private static InitialContext ctx;
    
    // definition of URL and port to connect - we have 2 nodes
    protected static String[][] namingURLs = new String[2][2];
    // will be defined by child testing class
    protected static String ARCHIVE_NAME = null;
    
    private static final Class<VMTester> VM_TESTER_INT = VMTester.class; 
    private static final String CONNECTION_STRING = "remote.connection.default";
    private static final String CONNECTION_HOST = CONNECTION_STRING + ".host";
    private static final String CONNECTION_PORT = CONNECTION_STRING + ".port";
    
    
    public static final String TESTER_JNDI_NAME = "NonClusteredStateless";
    
    static {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        try {
            ctx = new InitialContext(env);
        } catch (NamingException e) {
            log.error("Initial Context was not created properly", e);
        }
    }
    
    protected ContextSelector<EJBClientContext> setupEJBClientContextSelector(Properties properties) throws IOException {
         return EJBClientContextSelector
                .setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties", properties);
    }
    
    protected <T> String createJndiName(String beanName, Class<T> beanInterface, boolean isStateful) {
        return String.format("ejb:/%s/%s!%s%s", ARCHIVE_NAME, beanName, beanInterface.getName(), isStateful ? "?stateful" : "");
    }
    

    protected void stayLocalTest(String jndiName, boolean expectLocal, boolean expectLocalViaLookup) throws Exception {
        Properties env = new Properties();
        env.setProperty(CONNECTION_HOST, namingURLs[0][0]);
        env.setProperty(CONNECTION_PORT, namingURLs[0][1]);
        // env.setProperty("jnp.disableDiscovery", "true");
        previousSelector = setupEJBClientContextSelector(env);
        VMTester tester = (VMTester) ctx.lookup(createJndiName(TESTER_JNDI_NAME, VM_TESTER_INT, false));

        VMID local = tester.getVMID();
        Assert.assertNotNull("Got the local VMID", local);

        Properties env1 = new Properties();
        env1.setProperty(CONNECTION_HOST, namingURLs[1][0]);
        env1.setProperty(CONNECTION_PORT, namingURLs[1][1]);
        // env1.setProperty("jnp.disableDiscovery", "true");
        setupEJBClientContextSelector(env1);
        VMTester remote = (VMTester) ctx.lookup(jndiName);

        // This call instantiates the SFSB if needed
        VMID remoteID = remote.getVMID();
        Assert.assertNotNull("Got the remote VMID", remoteID);

        // Pass the proxy back to the server and invoke getVMID() on it
        VMID passThroughID = tester.getVMIDFromRemote(remote);
        Assert.assertNotNull("Got the remote VMID", passThroughID);

        if (expectLocal) {
            Assert.assertEquals("Call stayed local", local, passThroughID);
        } else {
            Assert.assertFalse("Call went remote", local.equals(passThroughID));
        }

        // TODO: for this to be working we would need to define outbound-connection for one of the servers
        // and define that outbound-connection in jboss-ejb-client.xml file
        // Tell the server to look up a proxy from node1 and invoke getVMID() on it
        /*
        passThroughID = tester.getVMIDFromRemoteLookup(namingURLs[1][0], namingURLs[1][1], jndiName);
        Assert.assertNotNull("Got the remote VMID", passThroughID);
        if (expectLocalViaLookup) {
            Assert.assertEquals("Call stayed local", local, passThroughID);
        } else {
            Assert.assertFalse("Call went remote", local.equals(passThroughID));
        }
        */
    }
}
