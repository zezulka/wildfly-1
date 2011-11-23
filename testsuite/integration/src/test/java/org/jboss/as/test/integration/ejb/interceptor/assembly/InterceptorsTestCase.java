/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.as.test.integration.ejb.interceptor.assembly;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * From TCK: ejb30/assembly/librarydirectory/defaultname
 * 
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class InterceptorsTestCase {
    private static Logger log = Logger.getLogger(InterceptorsTestCase.class);

    @ArquillianResource
    InitialContext ctx;
    
    @Deployment
    public static Archive<?> deployment()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "iterceptor-tck-test.jar")
                .addPackage(InterceptorsTestCase.class.getPackage());
                jar.addAsManifestResource(InterceptorsTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testIntercept() throws Exception {
        interceptTest("java:module/AssemblyBean");
    }

    @Test
    public void testInterceptAnnotated() throws Exception {
        interceptTest("java:module/AnnotatedAssemblyBean");
    }

    private void interceptTest(String jndi) throws Exception {
        AssemblyRemoteIF bean = (AssemblyRemoteIF) ctx.lookup(jndi);
        int actual = bean.remoteAdd(1, 2);
        Assert.assertEquals(203, actual);
    }

    @Test
    public void testInterceptDifferentMethods() throws Exception {
        interceptDifferentMethodsTest("java:module/AssemblyBean");
    }

    @Test
    public void testInterceptDifferentMethodsAnnotated() throws Exception {
        interceptDifferentMethodsTest("java:module/AnnotatedAssemblyBean");
    }

    private void interceptDifferentMethodsTest(String jndi) throws Exception {
        AssemblyRemoteIF bean = (AssemblyRemoteIF) ctx.lookup(jndi);
        int actual = bean.remoteAdd(1, 2);
        Assert.assertEquals(203, actual);

        actual = bean.remoteMultiply(0, 0);
        Assert.assertEquals(10000, actual);
    }

}