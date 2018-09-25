/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.context.cdi;

import javax.inject.Inject;
import javax.jms.JMSException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.TransactedMDB;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class MyInjectedJMSContextTestCase {
    private static final Logger log = Logger.getLogger(MyInjectedJMSContextTestCase.class);
    public static final String QUEUE_NAME = "java:/InjectedJMSContextTestCaseQueue";

    @Inject
    MessageSender sender;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "InjectedJMSContextTestCase.jar")
                .addClass(TransactedMDB.class)
                .addClass(MessageSender.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.jts \n"),"META-INF/MANIFEST.MF");
    }

    @Test
    public void sendAndReceiveWithContext() throws JMSException {
        log.info("Sending a message #1");
        sender.sendMessage("hello guys!");
        log.info("Sending a message #2");
        sender.sendMessage("hello guys #2!");
        log.info("Sending finished");
    }
}
