/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.jbas6161;

import java.util.Date;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;

@RunWith(Arquillian.class)
public class SameNamedJarUnitTestCase
{   	
	final private static String earNames[] = { "jbas6161-A", "jbas6161-B" };
	
	private Logger log = Logger.getLogger(SameNamedJarUnitTestCase.class);
	
    @Deployment(name="deploy1", order = 1)
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbas6161.jar");
        jar.addPackage(SameNamedJarUnitTestCase.class.getPackage());
        return ShrinkWrap.create(EnterpriseArchive.class, earNames[0] + ".ear").addAsModule(jar);
    }

    @Deployment(name="deploy2", order = 2)
    public static Archive<?> deployment2() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbas6161.jar");
        jar.addClasses(Greeter.class, SimpleSessionBean.class);
        // jar.addPackage(SameNamedJarUnitTestCase.class.getPackage());
        return ShrinkWrap.create(EnterpriseArchive.class, earNames[1] + ".ear").addAsModule(jar);
    }
	  
   /**
    * Do some simple beans to both session beans in each ear.
    */
   @Test
   @OperateOnDeployment("deploy1")
   public void testBeanCalls() throws Exception
   {
	  InitialContext ctx = new InitialContext();
      
      for(String earName : earNames)
      {
    	  String lookupStr = "java:global/" + earName + "/jbas6161/" + SimpleSessionBean.class.getSimpleName();
    	  log.info("JNDI lookup: " + lookupStr);
    	  Greeter bean = (Greeter) ctx.lookup(lookupStr);
    	  String name = new Date().toString();
    	  String actual = bean.sayHiTo(name);
    	  Assert.assertEquals("Hi " + name, actual);
      }
   }
}
