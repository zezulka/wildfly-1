/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
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
package org.jboss.as.test.integration.ee.enventry;

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
 * Migration test from EJB Testsuite (ejbthree-985) to AS7 [JIRA JBQA-5483]. 
 * Test to see if optional env-entry-value works (16.4.1.3).
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class OptionalEnvEntryTestCase
{
    private static final Logger log = Logger.getLogger(OptionalEnvEntryTestCase.class);
    
    @ArquillianResource
    InitialContext ctx;
    
    @Deployment
    public static Archive<?> deployment()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "optional-env-entry-test.jar")
                .addPackage(OptionalEnvEntryTestCase.class.getPackage());
                jar.addAsManifestResource(OptionalEnvEntryTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }
    
    
   private OptionalEnvEntry lookupBean() throws Exception
   {
      return (OptionalEnvEntry) ctx.lookup("java:module/OptionalEnvEntryBean");
   }
   
   @Test
   public void test() throws Exception
   {
      OptionalEnvEntry bean = lookupBean();
      Double actual = bean.getEntry();
      // 1.1 is defined in OptionalEnvEntryBean
      Assert.assertEquals(new Double(1.1), actual);
   }
   
   @Test
   public void testLookup() throws Exception
   {
      OptionalEnvEntry bean = lookupBean();
      bean.checkLookup();
   }
}
