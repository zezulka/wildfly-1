/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.ssl;

import java.io.File;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.client.selector.EJBClientContextSelectorChangingBean;
import org.jboss.as.test.integration.ejb.util.EJBClientContextSelector;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing ssl connection of ssl client.
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
// @ServerSetup(SSLRealmSetupTask.class)
@RunAsClient
public class SSLEJBRemoteClientTestCase {
    private static final Logger log = Logger.getLogger(SSLEJBRemoteClientTestCase.class);
    private static final String MODULE_NAME = "ssl-remote-ejb-client-test";

    @Deployment
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(StatelessBeanRemote.class, StatelessBean.class);        
        File testPackage = new File("/tmp", "ssl-remote-ejb-client-test.jar");
        jar.as(ZipExporter.class).exportTo(testPackage, true);
        return jar;
    }
    
    @Test
    public void test(@ArquillianResource ManagementClient managementClient) throws Exception {       
        // Defining remoting connector to use ssl security realm
        //SSLRealmSetupTask.setRemotingConnectorRealm(managementClient, SSLRealmSetupTask.SECURITY_REALM_NAME);
       // SSLRealmSetupTask.reload(managementClient);
        ContextSelector<EJBClientContext> previousSelector = null;

        try {
            // Defining for client where to find certificate
            log.info("ABS cert path: " + SSLRealmSetupTask.KEYSTORE_ABSOLUTE_PATH + SSLRealmSetupTask.TRUSTSTORE_CLIENT_FILENAME);
            System.setProperty("javax.net.ssl.trustStore", SSLRealmSetupTask.KEYSTORE_ABSOLUTE_PATH + SSLRealmSetupTask.TRUSTSTORE_CLIENT_FILENAME);
            System.setProperty("javax.net.ssl.trustStorePassword", SSLRealmSetupTask.KEYSTORE_PASSWORD);
            System.setProperty("javax.net.debug", "all"); 
            // System.setProperty("javax.net.ssl.keyStore", SSLRealmSetupTask.KEYSTORE_CLIENT_FILENAME);
         
            // Taken properties defined to use SSL
            // previousSelector = EJBClientContextSelector.setup(SSLRealmSetupTask.KEYSTORE_RELATIVE_PATH + File.separator + "jboss-ejb-client.properties");
            
            Properties env = new Properties();
            env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
            
            InitialContext ctx = new InitialContext(env);
            
            StatelessBeanRemote bean = (StatelessBeanRemote) ctx.lookup(String.format("ejb:/%s//%s!%s", 
                    MODULE_NAME, StatelessBean.class.getSimpleName(), StatelessBeanRemote.class.getName()));
            Assert.assertEquals("Remote connection of EJB client through SSL was not successful", StatelessBeanRemote.ANSWER, bean.sayHello());
        } finally {
            if(previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
            //SSLRealmSetupTask.setRemotingConnectorRealm(managementClient, SSLRealmSetupTask.PREVIOUS_SECURITY_REALM_NAME);
        }
    }
}
