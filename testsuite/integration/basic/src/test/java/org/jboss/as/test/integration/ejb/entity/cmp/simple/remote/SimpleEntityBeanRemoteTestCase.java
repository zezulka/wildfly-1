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
package org.jboss.as.test.integration.ejb.entity.cmp.simple.remote;

import java.util.Hashtable;

import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBHomeLocator;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleEntityBeanRemoteTestCase {
    private static Logger log = Logger.getLogger(SimpleEntityBeanRemoteTestCase.class);

    private static final String APP_NAME = "";
    private static final String MODULE_NAME = "simple-app"; // "simple-module";

    @Deployment
    public static Archive<?> deploy() {
        // final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addClasses(
                SimpleEntityBeanRemoteTestCase.class, 
                SimpleRemote.class, 
                SimpleRemoteBean.class, 
                SimpleRemoteHome.class
               );
        jar.addAsManifestResource(SimpleEntityBeanRemoteTestCase.class.getPackage(), "ejb-jar-remote.xml", "ejb-jar.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.naming \n"), "MANIFEST.MF");
        // ear.addAsModule(jar);
        // log.info(ear.toString(true));
        return jar;
    }
    
    @Test
    public void test() throws Exception {
        SimpleRemote bean = setUpEjb();
        
        bean.setAbc("abc");
        String abcString = bean.getAbc();
        log.info("abcString: " + abcString);
        
        tearDownEjb(bean);
    }
    
    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String,String> jndiProperties = new Hashtable<String,String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY,"org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }


    private SimpleRemoteHome getSimpleHome() {
        try {
            Object o = getInitialContext().lookup("ejb:" + APP_NAME + " /" + MODULE_NAME + "//SimpleEJB!" + SimpleRemoteHome.class.getName());
            SimpleRemoteHome ebh = (SimpleRemoteHome) javax.rmi.PortableRemoteObject.narrow(o, SimpleRemoteHome.class);
            return ebh;
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getSimpleHome: " + e.getMessage());
        }
        return null;
    }
    
    private <T extends EJBHome> T getHome(final Class<T> homeClass, final String beanName) {
        final EJBHomeLocator<T> locator = new EJBHomeLocator<T>(homeClass, APP_NAME, MODULE_NAME, beanName, "");
        return EJBClient.createProxy(locator);
    }


    public SimpleRemote setUpEjb() throws Exception {
        SimpleRemoteHome simpleHome = getSimpleHome();
        // SimpleRemoteHome simpleHome = getHome(SimpleRemoteHome.class, "SimpleEJB");
        SimpleRemote simple = null;

        try {
            simple = simpleHome.findByPrimaryKey("simple");
        } catch (Exception e) {
            log.info("No simple exists: " + simpleHome + ".\nException:  " + e.getMessage());
        }

        if (simple == null) {
            simple = simpleHome.create("ssimple");
        }
        String id = simple.getId();
        log.info("simple.getId(): " + id);
        return simple;
    }

    public void tearDownEjb(SimpleRemote bean) throws Exception {
        bean.remove();
    }
}
