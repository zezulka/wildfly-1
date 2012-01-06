/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.depends;

import java.util.logging.Logger;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Depends tags in jboss specific descriptor test.
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 * @author Alexey Loubyansky, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class EjbDependsUnitTestCase {

    private static final Logger log = Logger.getLogger(EjbDependsUnitTestCase.class.getName());

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejbdepends.ear");
        JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "ejbdepends1.jar");
        jar1.addClasses(MySession.class, MySessionBean.class, MySessionHome.class);
        jar1.addAsManifestResource(EjbDependsUnitTestCase.class.getPackage(), "ejb-jar-jar1.xml", "ejb-jar.xml");
        jar1.addAsManifestResource(EjbDependsUnitTestCase.class.getPackage(), "jboss-ejb3-jar1.xml", "jboss-ejb3.xml");
        JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "ejbdepends2.jar");
        jar2.addClasses(MySession.class, MySessionBean.class, MySessionHome.class);
        jar2.addAsManifestResource(EjbDependsUnitTestCase.class.getPackage(), "ejb-jar-jar2.xml", "ejb-jar.xml");
        jar2.addAsManifestResource(EjbDependsUnitTestCase.class.getPackage(), "jboss-ejb3-jar2.xml", "jboss-ejb3.xml");

        ear.addAsModule(jar1);
        ear.addAsModule(jar2);
        ear.addAsManifestResource(EjbDependsUnitTestCase.class.getPackage(), "application.xml", "application.xml");
        log.info(ear.toString(true));
        return ear;
    }

    @Test
    public void testJBAS8032() throws Exception {
        InitialContext ic = new InitialContext();
        Assert.assertNotNull(ic.lookup("MySession"));
        Assert.assertNotNull(ic.lookup("MySession2"));
        Assert.assertNotNull(ic.lookup("MySession3"));
    }
}
