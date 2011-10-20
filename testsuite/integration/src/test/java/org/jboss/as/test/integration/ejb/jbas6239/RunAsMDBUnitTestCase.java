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
package org.jboss.as.test.integration.ejb.jbas6239;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

// import org.jboss.test.JBossTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.entity.cmp2.SimpleUnitTestCase;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.formatter.Formatters;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.Test;

/**
 * Make sure the run-as on a MDB is picked up.
 * 
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @version $Revision: 82920 $
 */
@RunWith(Arquillian.class)
public class RunAsMDBUnitTestCase 
{
   private static Logger log = Logger.getLogger(RunAsMDBUnitTestCase.class);
   
   @Deployment
   public static Archive<?> deploy() {
       JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbas6239.jar");
       jar.addPackage(RunAsMDBUnitTestCase.class.getPackage());
       jar.addAsManifestResource("ejb3/jbas6239/hornetq-jms.xml", "hornetq-jms.xml");
       jar.addAsManifestResource("ejb3/jbas6239/roles.properties", "roles.properties");
       jar.addAsManifestResource("ejb3/jbas6239/users.properties", "users.properties");       
       // log.error(jar.toString(Formatters.VERBOSE));
       return jar;
   }
   

   protected <T> T lookup(String name, Class<T> cls) throws Exception
   {
      InitialContext ctx = new InitialContext();
      Object obj = ctx.lookup(name);
      // log.warn(obj.getClass().getSimpleName() + "\n" + obj.toString());
      return cls.cast(obj);
   }
   
   @Test
   public void testSendMessage() throws Exception
   {    
      ConnectionFactory connFactory = lookup("java:/ConnectionFactory", ConnectionFactory.class);
      Connection conn = connFactory.createConnection();
      conn.start();
      Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
      TemporaryQueue replyQueue = session.createTemporaryQueue();
      TextMessage msg = session.createTextMessage("Hello world");
      msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
      msg.setJMSReplyTo(replyQueue);
      // Queue queue = lookup("java:/queue/mdbtest", Queue.class);
      Queue queue = lookup("java:/queue/test", Queue.class);
      MessageProducer producer = session.createProducer(queue);
      producer.send(msg);
      MessageConsumer consumer = session.createConsumer(replyQueue);
      Message replyMsg = consumer.receive(5000);
      Assert.assertNotNull(replyMsg);
      if(replyMsg instanceof ObjectMessage)
      {
         Exception e = (Exception) ((ObjectMessage) replyMsg).getObject();
         throw e;
      }

      Assert.assertTrue(replyMsg instanceof TextMessage); //?
      String actual = ((TextMessage) replyMsg).getText();
      Assert.assertEquals("SUCCESS", actual);
      
      // TODO: check stateless.state
      
      consumer.close();
      producer.close();
      session.close();
      conn.stop();
   }
}
