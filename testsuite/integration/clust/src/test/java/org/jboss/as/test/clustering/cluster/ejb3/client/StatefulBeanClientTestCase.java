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

package org.jboss.as.test.clustering.cluster.ejb3.client;

import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Ondrej Chaloupka
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulBeanClientTestCase {
    private static final Logger log = Logger.getLogger(StatefulBeanClientTestCase.class);
    private static final String ARCHIVE_NAME = "client-stateful";
    private static RemoteEJBDirectory context;
    
    private static boolean node1Started;
    private static boolean node2Started;
    private static String node1;
    private static String node2;
    private static String nodeCalled;
    
    private static String testingString = "Hi";
    private static StatefulBeanRemote statefulBean; 
    
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void startUp() throws NamingException {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
        context = new RemoteEJBDirectory(ARCHIVE_NAME);
    }

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        WebArchive jar = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        jar.addClasses(StatefulBean.class, StatefulBeanRemote.class, NodeNameUtil.class, NodeInfoServlet.class);
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    @InSequence(1)
    public void testArquillianWorkaround() {
        // Container is unmanaged, need to start manually.
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        // TODO: This is nasty. I need to start it to be able to inject it later and then stop it again!
        // https://community.jboss.org/thread/176096
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    @Test
    @InSequence(2)
    public void lookupStatefulBean(
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource() @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2)
            throws Exception {

        node1Started = true;
        node2Started = true;
        
        String servletUrl = "nodename";
        String askingFor = baseURL1.toString() + servletUrl;
        log.info("URL where we ask: " + askingFor);
        node1 = HttpRequest.get(askingFor, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        log.info("URL1 nodename: " + node1);
        node2 = HttpRequest.get(baseURL2.toString() + servletUrl, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        log.info("URL2 nodename: " + node2);

        // getting context for remote client
        EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");

        statefulBean = context.lookupStateful(StatefulBean.class, StatefulBeanRemote.class);
        nodeCalled = statefulBean.createGetString(testingString);
        log.info("Bean called on node: " + nodeCalled);
        Assert.assertEquals("Hi", statefulBean.getString());

        // Shutdown operation
        ModelNode address = new ModelNode();
        address.add(DEPLOYMENT, ARCHIVE_NAME + ".war");
        address.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(UNDEPLOY);
        // ModelNode operation = new ModelNode();
        // operation.get(OP).set(SHUTDOWN);

        if(nodeCalled.equals(node2)) {
            // deployer.undeploy(DEPLOYMENT_2);
            // controller.stop(CONTAINER_2);
            ModelNode result = client2.getControllerClient().execute(operation);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            // Assert.assertFalse(waitForShutdown(client2));
            log.info(node2 + " went away");
            node2Started = false;
        } else {
            // deployer.undeploy(DEPLOYMENT_1);
            // controller.stop(CONTAINER_1);
            ModelNode result = client1.getControllerClient().execute(operation);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            // Assert.assertFalse(waitForShutdown(client1));
            log.info(node1 + " went away");
            node1Started = false;
        }
    }
    
    private boolean waitForShutdown(ManagementClient client) throws InterruptedException {
        int loop = 20;
        int wait_ms = 800;
        int currentLoop = 0;
        while(client.isServerInRunningState() && currentLoop < loop) {
            Thread.sleep(wait_ms);
            currentLoop++;
        }
        return client.isServerInRunningState();
    }
    
    @Test
    @InSequence(3) 
    public void checkClientContextConnectingToServer(
            ) throws InterruptedException {
        
        Assert.assertEquals(testingString, statefulBean.getString());
        Thread.sleep(1000);
        Assert.assertEquals(testingString, statefulBean.getString());
        
        // we need to have started container to be able to undeploy our test app
        if(node1Started) {
            deployer.undeploy(CONTAINER_1);
        }
        if(node2Started) {
            deployer.undeploy(CONTAINER_2);
        }
        // deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        // deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);
    }
}
