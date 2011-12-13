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

package org.jboss.as.test.integration.ejb.ejb2.ejb3refer.ear;

import javax.naming.InitialContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for EJB3.0/EJB2.1 references
 * 
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@RunWith(Arquillian.class)
public class EarTestCase {
    private static final Logger log = Logger.getLogger(EarTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "multideploy-reference21_31-test.ear");

        final JavaArchive jar2 = ShrinkWrap.create(JavaArchive.class, "multideploy-ejb3.jar").addClasses(Test3.class,
                Test3Business.class, Test3Home.class);
        jar2.addAsManifestResource(EarTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss.xml");
        jar2.addAsManifestResource(EarTestCase.class.getPackage(), "ejb-jar-ejb3.xml", "ejb-jar.xml");

        final JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "multideploy-ejb2.jar").addClasses(Test2.class,
                Test2Bean.class, Test2Home.class, Test3Business.class, Test3Home.class);
        jar1.addAsManifestResource(EarTestCase.class.getPackage(), "jboss-ejb2.xml", "jboss.xml");
        jar1.addAsManifestResource(EarTestCase.class.getPackage(), "ejb-jar-ejb2.xml", "ejb-jar.xml");

        log.info(ear.toString(true));
        ear.addAsModule(jar1);
        ear.addAsModule(jar2);
        return ear;
    }

    @Test
    public void testEjbInjection2() throws Exception {

        Test3Home test3Home = (Test3Home) ctx.lookup("Test3/home");
        Test3 test3 = test3Home.create();
        Assert.assertNotNull(test3);
        test3.testAccess();

        Test2Home home = (Test2Home) ctx.lookup("Test2");
        Assert.assertNotNull(home);
        Test2 test2 = home.create();
        Assert.assertNotNull(test2);
        test2.testAccess();
    }

    public void testEjbInjection3() throws Exception {

        Test3Business test3 = (Test3Business) ctx.lookup("Test3Remote");
        Assert.assertNotNull(test3);
        test3.testAccess();

        Test2Home home = (Test2Home) ctx.lookup("Test2");
        Assert.assertNotNull(home);
        Test2 test2 = home.create();
        Assert.assertNotNull(test2);
        test2.testAccess();
    }
}
