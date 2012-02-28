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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.passivation;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.clustering.util.EJBClientContextSelectorUtil;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.dmr.ModelNode;
import org.jboss.ejb.client.ClusterContext;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * FIXME: add comment!!!
 * 
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClusterPassivationTestCase {
    private static Logger log = Logger.getLogger(ClusterPassivationTestCase.class);
    public static String ARCHIVE_NAME = "cluster-passivation-test";
    private static String CLUSTER_NAME = "ejb";
    
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    @BeforeClass
    public static void setUp() throws NamingException {
        Properties sysprops = System.getProperties();
        System.out.println("System properties:\n" + sysprops);
    }
    
    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        Archive<?> archive = createDeployment();
        return archive;
    }

    private static Archive<?> createDeployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addPackage(ClusterPassivationTestCase.class.getPackage());
        System.out.println(war.toString(true));
        return war;
    }
    
    private static ModelNode getAddress() {
        ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("cluster-passivation-store", "infinispan");
        address.protect();
        return address;
    }
    
    private static void setPassivationAttributes(ModelControllerClient client) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("max-size");
        operation.get("value").set(1);
        ModelNode result = client.execute(operation);
        log.info("modelnode operation write attribute max-size=1: " + result);
         /* Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("idle-timeout");
        operation.get("value").set(1);
        result = client.execute(operation);
        log.info("modelnode operation write-attribute idle-timeout=1: " + result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString()); */
    }

    private static void unsetPassivationAttributes(ModelControllerClient client) throws Exception {
        ModelNode address = getAddress();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("max-size");
        client.execute(operation);
        /* operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get(OP_ADDR).set(address);
        operation.get("name").set("idle-timeout");
        result = client.execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        client.close(); */
    }
    
    /**
     * Sets up the EJB client context to use a selector which processes and sets up EJB receivers
     * based on this testcase specific jboss-ejb-client.properties file
     *
     * @return
     * @throws java.io.IOException
     */
    private ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        // Properties properties = new Properties();
        // properties.put("remote.cluster.ejb.clusternode.selector", MyClusterNodeSelector.class.getName());
        EJBClientContextSelectorUtil contextSelectorUtil = new EJBClientContextSelectorUtil();
        log.info("EJBClientContextSelectorUtil created");
        return contextSelectorUtil.setupEJBClientContextSelector();
    }
    
    // TESTS ----------------------------------------------
    @Test
    @InSequence(1)
    public void testArquillianWorkaround() {
        // Container is unmanaged, need to start manually.
        // This is a little hacky - need for URL and client injection. @see https://community.jboss.org/thread/176096
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    @Test
    @InSequence(2)
    public void testRestart(
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) URL baseURL1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) URL baseURL2,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_1) ManagementClient client1,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) ManagementClient client2,
            @ArquillianResource @OperateOnDeployment(DEPLOYMENT_2) InitialContext ctx)
            throws Exception {

        // set passivation
        setPassivationAttributes(client1.getControllerClient());
        setPassivationAttributes(client2.getControllerClient());

        // Loading context from file to get ejb:// remote context
        final ContextSelector<EJBClientContext> previousSelector = setupEJBClientContextSelector(); // setting context from .properties file
        final String jndiName = "ejb:" + "" + "/" + ARCHIVE_NAME + "//" + StatefulBean.class.getSimpleName() + "!" + StatefulBeanRemote.class.getName() + "?stateful";
        final StatefulBeanRemote statefulBeanRemote = (StatefulBeanRemote) ctx.lookup(jndiName);
        
        // Associtation of node names to deployment,container names and client context
        Map<String, String> node2deployment = new HashMap<String, String>();
        Map<String, String> node2container = new HashMap<String, String>();
        Map<String, ManagementClient> node2client = new HashMap<String, ManagementClient>();
        String servletUrl = "nodename";        
        String nodeName1 = HttpRequest.get(baseURL1.toString() + servletUrl, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        node2deployment.put(nodeName1, DEPLOYMENT_1);
        node2container.put(nodeName1, CONTAINER_1);
        node2client.put(nodeName1, client1);
        log.info("URL1 nodename: " + nodeName1);
        String nodeName2 = HttpRequest.get(baseURL2.toString() + servletUrl, HTTP_REQUEST_WAIT_TIME_S, TimeUnit.SECONDS);
        node2deployment.put(nodeName2, DEPLOYMENT_2);
        node2container.put(nodeName2, CONTAINER_2);
        node2client.put(nodeName2, client2);
        log.info("URL2 nodename: " + nodeName2);
        
        
        // Calling on server one
        int clientNumber = 40;
        String calledNodeFirst = statefulBeanRemote.setNumber(clientNumber);
        statefulBeanRemote.setPassivationNode(calledNodeFirst);
        log.info("Called node name first: " + calledNodeFirst);
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation
        statefulBeanRemote.incrementNumber(); //41
               
        // A small hack - deleting node (by name) from cluster which this client knows. 
        // It means that the next request (ejb call) will be passed to another server 
        EJBClientContext.requireCurrent().getClusterContext(CLUSTER_NAME).removeClusterNode(calledNodeFirst);
        // Calling on another (second) server
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber()); //41
        // this was redefined in @PrePassivate method on first server - checking whether second server knows about it
        Assert.assertEquals(calledNodeFirst, statefulBeanRemote.getPassivatedBy());  // depends on call of method setPassivationNode()
        String calledNodeSecond = statefulBeanRemote.incrementNumber(); //42
        statefulBeanRemote.setPassivationNode(calledNodeSecond);
        log.info("Called node name second: " + calledNodeSecond);
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation
        
        // Resetting cluster context to know both cluster nodes
        setupEJBClientContextSelector();
        
        // Waiting for cluster context - it could take some time for client to get info from cluster nodes
        int counter = 0;
        EJBClientContext ejbClientContext = EJBClientContext.requireCurrent();
        ClusterContext clusterContext = null;
        do {
            clusterContext = ejbClientContext.getClusterContext(CLUSTER_NAME);
            counter--;
            Thread.sleep(CLUSTER_ESTABLISHMENT_WAIT_MS);
        } while(clusterContext == null && counter < CLUSTER_ESTABLISHMENT_LOOP_COUNT);
        Assert.assertNotNull("Cluster context for " + CLUSTER_NAME + " was not taken in " + (CLUSTER_ESTABLISHMENT_LOOP_COUNT*CLUSTER_ESTABLISHMENT_WAIT_MS) + " ms", clusterContext);
        
        // Stopping node #2
        unsetPassivationAttributes(node2client.get(calledNodeSecond).getControllerClient());
        deployer.undeploy(node2deployment.get(calledNodeSecond));
        controller.stop(node2container.get(calledNodeSecond));

        // We killed second node and we check the value on first node
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber()); //42
        // Calling on first server
        String calledNode = statefulBeanRemote.incrementNumber(); //43
        // Checking called node and set number
        Assert.assertEquals(calledNodeFirst, calledNode);
        Assert.assertEquals(calledNodeSecond, statefulBeanRemote.getPassivatedBy()); // depends on call of method setPassivationNode()
        Thread.sleep(WAIT_FOR_PASSIVATION_MS); // waiting for passivation
        Assert.assertEquals(++clientNumber, statefulBeanRemote.getNumber());
        
        // returning to the previous context selector, @see {RemoteEJBClientDDBasedSFSBFailoverTestCase}
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
    }

    @Test
    @InSequence(100)
    public void undeployArquillianWorkaround(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2) throws Exception {
        if(client1 != null && client1.isServerInRunningState()) {
            log.info("Cleaning: shutting down container: " + CONTAINER_1);
            unsetPassivationAttributes(client1.getControllerClient());
            deployer.undeploy(DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
        }
        if(client2 != null && client2.isServerInRunningState()) {
            log.info("Cleaning: shutting down container: " + CONTAINER_2);
            unsetPassivationAttributes(client2.getControllerClient());
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
    }
}
