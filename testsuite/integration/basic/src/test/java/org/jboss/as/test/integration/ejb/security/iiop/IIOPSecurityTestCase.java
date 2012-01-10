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

package org.jboss.as.test.integration.ejb.security.iiop;

import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Part of AS5->AS7 testsuite migration [JBQA-5275] (ejb3/iiop/unit/IiopRemoteUnitTestCase).
 * 
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IIOPSecurityTestCase {
    private static final Logger log = Logger.getLogger(IIOPSecurityTestCase.class.getName());

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-iiop-security.jar");
        jar.addPackage(IIOPSecurityTestCase.class.getPackage());
        jar.addAsResource(IIOPSecurityTestCase.class.getPackage(), "users.properties", "users.properties");
        jar.addAsResource(IIOPSecurityTestCase.class.getPackage(), "roles.properties", "roles.properties");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testSecurity() throws Exception {
        final SecurityClient securityClient = SecurityClientFactory.getSecurityClient();
        securityClient.setSimple("user1", "pass1");
        try {
            // login
            securityClient.login();

            final Properties prope = new Properties();
            final InitialContext context = new InitialContext(prope);
            Object obj = context.lookup("corbaname:iiop:localhost:3528#ejb3-iiop-security/MySessionBean");
            System.err.println(obj.getClass());
            MySessionHome sessionHome = (MySessionHome) PortableRemoteObject.narrow(obj, MySessionHome.class);
            MySession session = sessionHome.create();
            Assert.assertNotNull(session);
            String actual = session.getWhoAmI();
            System.err.println("whoAmI = " + actual);
            Assert.assertEquals(actual, "user1");

        } finally {
            securityClient.logout();
        }
    }

}
