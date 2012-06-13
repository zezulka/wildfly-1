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

package org.jboss.as.test.clustering.cluster.ejb3.stateful.loadbalancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.EJBDirectory;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.RemoteEJBDirectory;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * Testing how stateful beans are created on servers in equal numbers (basic strategy is random).
 * 
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class StatefulLoadbalancingConcurrentTestCase {
    private static final Logger log = Logger.getLogger(StatefulLoadbalancingConcurrentTestCase.class);

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;

    public static final String ARCHIVE_NAME = "stateful-loadbalancing-test";
    private static EJBDirectory context;
    private static ContextSelector<EJBClientContext> previousSelector;
    private static List<StatefulBeanRemote> beanList = new ArrayList<StatefulBeanRemote>();
    // number of percent to tolerate that the load balancing works right
    private static final double PERCENTAGE_TOLERANCE = 10.0;
    // number of newly created stateful beans in test
    private static final int NUMBER_OF_BEANS = 400;
    private static final int NUMBER_OF_THREADS = 12;
    private static AtomicInteger threadNumber = new AtomicInteger(0);

    @BeforeClass
    public static void printSysProps() throws Exception {
        Properties sysprops = System.getProperties();
        log.info("System properties:\n" + sysprops);
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

    @Deployment(name = DEPLOYMENT_3, managed = false, testable = false)
    @TargetsContainer(CONTAINER_3)
    public static Archive<?> createDeploymentForContainer3() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_4, managed = false, testable = false)
    @TargetsContainer(CONTAINER_4)
    public static Archive<?> createDeploymentForContainer4() {
        return createDeployment();
    }

    private static JavaArchive createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(StatefulBeanRemote.class, StatefulBean.class, NodeNameGetter.class);
        System.out.println(jar.toString(true));
        return jar;
    }

    /**
     * Start servers whether their are not started.
     */
    private void startServers() {
        log.info("Starting server: " + CONTAINER_3);
        controller.start(CONTAINER_3);
        deployer.deploy(DEPLOYMENT_3);

        log.info("Starting server: " + CONTAINER_1);
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);

        log.info("Starting server: " + CONTAINER_2);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);

        /*log.info("Starting server: " + CONTAINER_4);
        controller.start(CONTAINER_4);
        deployer.deploy(DEPLOYMENT_4); */
    }

    private void validateBalancing(Map<String, Integer> nodeNameNumberOfCalls) {
        int totalCalls = 0;
        int numberOfNodes = nodeNameNumberOfCalls.size();
        double supposedPercentage = numberOfNodes <= 0 ? 0 : 100.0 / numberOfNodes;
        String supposedPercentageInString = String.format("%1.2f %%", supposedPercentage);

        for (String nodeName : nodeNameNumberOfCalls.keySet()) {
            totalCalls += nodeNameNumberOfCalls.get(nodeName);
        }
        StringBuilder sb = new StringBuilder();
        for (String nodeName : nodeNameNumberOfCalls.keySet()) {
            int currentCalls = nodeNameNumberOfCalls.get(nodeName);
            double currentPercentage = ((double) currentCalls / totalCalls) * 100;
            String currentPercentageInString = String.format("%1.2f %%", currentPercentage);
            Assert.assertTrue("Node " + nodeName + " has " + currentPercentageInString + " created beans from all "
                    + totalCalls + " but it does not match with supposed percentage " + supposedPercentageInString + "+-"
                    + PERCENTAGE_TOLERANCE, (currentPercentage >= supposedPercentage - PERCENTAGE_TOLERANCE)
                    && (currentPercentage <= supposedPercentage + PERCENTAGE_TOLERANCE));
            sb.append(String.format("[%s: %1.2f %%] ", nodeName, currentPercentage));
        }
        log.info("validateBalancing[validation loadbalancing, vvalidate](): " + sb);
    }

    private final class CallBeanTask implements Callable<Map<String, Integer>> {
        public Map<String, Integer> call() {
            int currentThreadNumber = threadNumber.incrementAndGet();
            log.info("Thread: " + currentThreadNumber + " was started to call stateful beans.");
            Map<String, Integer> nodeNameMap = new HashMap<String, Integer>();
            try {
                for (int i = 0; i < NUMBER_OF_BEANS; i++) {
                    StatefulBeanRemote bean = context.lookupStateful(StatefulBean.class, StatefulBeanRemote.class);
                    String nodeName = bean.getNodeName();
                    int number = nodeNameMap.get(nodeName) == null ? 1 : nodeNameMap.get(nodeName) + 1;
                    nodeNameMap.put(bean.getNodeName(), number);
                    synchronized (beanList) {
                        beanList.add(bean);
                    }
                }
            } catch (Exception e) {
                // in case of error we just throw runtime exception to be shown
                throw new RuntimeException(e);
            }

            return nodeNameMap;
        }
    }

    @Test
    @InSequence(1)
    public void testBalancing() throws Exception {
        startServers();
        previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateful/loadbalance/jboss-ejb-client.properties");

        beanList = new ArrayList<StatefulBeanRemote>();
        ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
        CompletionService<Map<String, Integer>> pool = new ExecutorCompletionService<Map<String, Integer>>(threadPool);
        for(int i=0; i<NUMBER_OF_THREADS; i++) { // run threads
            pool.submit(new CallBeanTask());
        }

        Map<String, Integer> result = new HashMap<String, Integer>();
        for(int i=0; i<NUMBER_OF_THREADS; i++) { // taken results from all the threads
            result.putAll(pool.take().get());
        }

        log.info("our list size is -------->:" + result.size());
        Assert.assertEquals(NODES.length + " servers were started but the test worked with " + result.size(), NODES.length-1,
                result.size());
        validateBalancing(result);

        threadPool.shutdown();

        // Call beans once again
        for (StatefulBeanRemote bean : beanList) {
            log.info("Second call from: " + bean.getNodeName());
        }
        // Remove beans
        for (StatefulBeanRemote bean : beanList) {
            bean.remove();
        }
    }

    @Test
    @InSequence(100)
    public void stopAndClean(@OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2,
            @OperateOnDeployment(DEPLOYMENT_3) @ArquillianResource ManagementClient client3
            // ,@OperateOnDeployment(DEPLOYMENT_4) @ArquillianResource ManagementClient client4
            ) throws Exception {
        log.info("Stop&Clean...");
        // returning to the previous context selector, @see {RemoteEJBClientDDBasedSFSBFailoverTestCase}
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
        // unset & undeploy & stop
        if (client1 != null && client1.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_1);
            controller.stop(CONTAINER_1);
        }
        if (client2 != null && client2.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_2);
            controller.stop(CONTAINER_2);
        }
        if (client3 != null && client3.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_3);
            controller.stop(CONTAINER_3);
        }
        /*if (client4 != null && client4.isServerInRunningState()) {
            deployer.undeploy(DEPLOYMENT_4);
            controller.stop(CONTAINER_4);
        }*/
    }
}
