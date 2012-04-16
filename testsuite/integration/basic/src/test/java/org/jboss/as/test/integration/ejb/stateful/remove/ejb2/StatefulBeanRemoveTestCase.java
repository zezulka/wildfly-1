/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.remove.ejb2;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class StatefulBeanRemoveTestCase {
    // private static final Logger log = Logger.getLogger(RemoteEJBClientStatefulBeanFailoverTestCase.class);

    private static final String PROPERTIES_FILE = "jboss-ejb-client.properties";
    private static final String ARCHIVE_NAME = "ejb2-failover-test";
    private static final String ARCHIVE_NAME_SINGLE = "single";
    
    private static InitialContext context;
    
    @Deployment(name = "single", managed = true, testable = false, order = 1)
    public static Archive<?> createDeploymentForContainer1Singleton() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SINGLE + ".jar");
        jar.addClasses(CounterSingleton.class, CounterSingletonRemote.class);
        return jar;
    }
    
    @Deployment(name = "ejb", managed = true, testable = true, order = 2)
    public static Archive<?> createDeploymentForContainer1() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(CounterBean.class, CounterRemote.class, CounterRemoteHome.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + ARCHIVE_NAME_SINGLE + ".jar \n"), "MANIFEST.MF");
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(env);
    }
   
    @Test
    @OperateOnDeployment("ejb")
    public void testLocalCall(@ArquillianResource InitialContext ctx) throws Exception {
        remove(ctx, "java:global/");
    }

    @Test
    @RunAsClient
    public void testRemoteCall() throws Exception {
        final ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup(PROPERTIES_FILE);
        try {
            remove(context, "ejb:/");
        } finally {
            // reset the selector
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }
    }
    
    /**
     * Implementation of defined abstract tests above.  
     */
    protected void remove(InitialContext iniCtx, String lookupPrefix) throws Exception {
            CounterRemoteHome home = (CounterRemoteHome) iniCtx.lookup(lookupPrefix + ARCHIVE_NAME + "/" + CounterBean.class.getSimpleName() + "!"
                    + CounterRemoteHome.class.getName());
            CounterRemote remoteCounter = home.create();
            Assert.assertNotNull(remoteCounter);
            
            final CounterSingletonRemote destructionCounter = (CounterSingletonRemote) iniCtx.lookup(lookupPrefix + ARCHIVE_NAME_SINGLE + "/" 
                    + CounterSingleton.class.getSimpleName() + "!" + CounterSingletonRemote.class.getName());
            destructionCounter.resetDestroyCount();
            
            final int result = remoteCounter.increment();

            Assert.assertNotNull("Result from remote stateful counter was null", result);
            Assert.assertEquals("Unexpected count from remote counter", 1, result);
            Assert.assertEquals("Nothing should have been destroyed yet", 0, destructionCounter.getDestroyCount());
            
            home.remove(remoteCounter.getHandle());
            Assert.assertEquals("SFSB was not destroyed", 1, destructionCounter.getDestroyCount());
            destructionCounter.resetDestroyCount();
            
            home = (CounterRemoteHome) iniCtx.lookup(lookupPrefix + ARCHIVE_NAME + "/" + CounterBean.class.getSimpleName() + "!"
                    + CounterRemoteHome.class.getName());
            remoteCounter = home.create();
            Assert.assertNotNull(remoteCounter);
            
            remoteCounter.remove();
            Assert.assertEquals("SFSB was not destroyed", 1, destructionCounter.getDestroyCount());
    }
}
