/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.exception.chaining;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (EPCPropagation) to AS7 [JIRA JBQA-5483]. Chaining several exceptions through beans.
 * 
 * @author William DeCoste, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class ExceptionTestCase {

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        jar.addPackage(ExceptionTestCase.class.getPackage());
        return jar;
    }
    
    @Test
    public void testException() throws Exception {
        InitialContext jndiContext = new InitialContext();

        Foo1 foo = (Foo1) jndiContext.lookup("java:module/FooBean1");
        Assert.assertNotNull(foo);

        try {
            foo.bar();
            Assert.assertTrue(false);
        } catch (Throwable t) {
            t.printStackTrace();
            Assert.assertTrue(t instanceof FooException1);
        }

        String status = foo.getStatus();
        Assert.assertEquals("Caught FooException1", status);
    }
}
