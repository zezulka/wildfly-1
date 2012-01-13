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

package org.jboss.as.test.integration.deployment.jarear;

import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JarEarTestCase {
    private static final Logger log = Logger.getLogger(JarEarTestCase.class.getName());

    @Deployment(name = "ejb", order = 1)
    public static Archive<?> deployEjbs() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3.jar");
        jar.addClasses(Bean.class);
        log.info(jar.toString(true));
        return jar;
    }
    
    @Deployment(name = "ear", order = 2) 
    public static Archive<?> deployEar() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejb3.ear");
        
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3.jar");
        jar.addClasses(Bean.class);
        
        ear.addAsModule(jar);
        
        ear.addAsManifestResource(JarEarTestCase.class.getPackage(), "application.xml", "application.xml");
        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void testEJBServletEar() throws Exception {
        Assert.assertEquals("OK", "OK");
    }
}
