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

package org.jboss.as.test.multinode.problem;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
/**
 * A simple EJB Remoting transaction context propagation in JTS style from one AS7 server to another.
 *
 * @author Stuart Douglas
 * @author Ivo Studensky
 */
@RunWith(Arquillian.class)
public class TestTestCase {

    public static final String SERVER_DEPLOYMENT = "server";
    public static final String CLIENT_DEPLOYMENT = "client";

    @Deployment(name = "server", testable = false)
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SERVER_DEPLOYMENT + ".jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class, StatefulLocal.class);
        return jar;
    }

    @Deployment(name = "client", testable = true)
    @TargetsContainer("multinode-client")
    public static Archive<?> clientDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, CLIENT_DEPLOYMENT + ".jar");
        jar.addClasses(StatefulBean.class, StatefulRemote.class, StatefulLocal.class, CallingClient.class, TestTestCase.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteClientCall(@ArquillianResource InitialContext ctx) throws Exception {
        CallingClient bean = (CallingClient) ctx.lookup("java:module/" + CallingClient.class.getSimpleName());
        Assert.assertNotNull(bean);
        int methodCount = bean.call();
        Assert.assertEquals(1, methodCount);
    }
    
    @Test
    @OperateOnDeployment("client")
    public void testStatelessRemoteDirectCall(@ArquillianResource InitialContext ctx) throws Exception {
        StatefulRemote bean = (StatefulRemote) ctx.lookup("java:module/" + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName());
        Assert.assertNotNull(bean);
        int methodCount = bean.call();
        Assert.assertEquals(1, methodCount);
    }
}
