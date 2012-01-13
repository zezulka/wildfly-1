/*
* JBoss, Home of Professional Open Source
* Copyright 2012, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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

package org.jboss.as.test.integration.deployment.multipleear;

import java.util.Hashtable;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for:
 * https://jira.jboss.org/jira/browse/JBAS-7760 
 * https://jira.jboss.org/jira/browse/JBAS-7789
 * 
 * Tests that multiple EAR files containing a EJB2.x jar with the same name deploy fine without throwing any
 * InstanceAlreadyExists exception while registering the deployment as a MBean.
 * 
 * Part of ejb tests migration task [JBQA-5275] (deployment/jbas7760).
 * 
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MultipleEarWithSameEjbModuleNameTestCase {
    private static final Logger log = Logger.getLogger(MultipleEarWithSameEjbModuleNameTestCase.class.getName());

    private static final String EAR_ONE_NAME = "earone";
    private static final String EAR_TWO_NAME = "eartwo";
    
    @Deployment(name = "appone", order = 1) 
    public static Archive<?> deployAppOne() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_ONE_NAME + ".ear");
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(AppOneEJB2xHome.class, AppOneEJB2xImpl.class, AppOneEJB2xRemote.class);
        jar.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "appone-ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "appone-jboss-ejb3.xml", "jboss-ejb3.xml");
        
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, "appone.war");
        war.addAsWebInfResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "appone-web.xml", "appone-web.xml");
        
        ear.addAsModule(jar);
        ear.addAsModule(war);
        
        ear.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "appone-application.xml", "application.xml");
        log.info(ear.toString(true));
        return ear;
    }

    @Deployment(name = "apptw", order = 2) 
    public static Archive<?> deployAppTwo() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_TWO_NAME + ".ear");
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(AppTwoEJB2xHome.class, AppTwoEJB2xImpl.class, AppTwoEJB2xRemote.class);
        jar.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "apptwo-ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "apptwo-jboss-ejb3.xml", "jboss-ejb3.xml");
        
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, "apptwo.war");
        war.addAsWebInfResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "apptwo-web.xml", "appone-web.xml");
        
        ear.addAsModule(jar);
        ear.addAsModule(war);
        
        ear.addAsManifestResource(MultipleEarWithSameEjbModuleNameTestCase.class.getPackage(), "apptwo-application.xml", "application.xml");
        log.info(ear.toString(true));
        return ear;
    }
    
    private InitialContext getInitialContext() throws NamingException {
        final Hashtable<String,String> jndiProperties = new Hashtable<String,String>();
        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY,"org.jboss.as.naming.InitialContextFactory");
        jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(jndiProperties);
    }
    
    /**
     * Test that the 2 ears containing a EJB2.x jar with the same name deploys fine
     * 
     * @throws Exception
     */
    @Test
    public void testDeploymentOfSameEjbJarNameInMultipleEar() throws Exception {
        AppOneEJB2xHome appOneHome = (AppOneEJB2xHome) getInitialContext().lookup("ejb:" + EAR_ONE_NAME + "/ejb//AppOneBean!" + AppOneEJB2xHome.class.getName());
        AppOneEJB2xRemote appOneRemote = (AppOneEJB2xRemote) PortableRemoteObject.narrow(appOneHome.create(),
                AppOneEJB2xRemote.class);
        
        // just test a simple invocation
        appOneRemote.doNothing();

        // do the same with the other app
        AppTwoEJB2xHome appTwoHome = (AppTwoEJB2xHome) getInitialContext().lookup("ejb:" + EAR_TWO_NAME + "/ejb//AppTwoBean!" + AppTwoEJB2xHome.class.getName());
        AppTwoEJB2xRemote appTwoRemote = (AppTwoEJB2xRemote) PortableRemoteObject.narrow(appTwoHome.create(),
                AppTwoEJB2xRemote.class);

        // just test a simple invocation
        appTwoRemote.doNothing();

    }
}
