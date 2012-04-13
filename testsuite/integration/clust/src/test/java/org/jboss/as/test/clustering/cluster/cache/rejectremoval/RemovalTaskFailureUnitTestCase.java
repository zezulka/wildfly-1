/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.clustering.cluster.cache.rejectremoval;


import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.DMRUtil;
import org.jboss.as.test.clustering.EJBClientContextSelector;
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

/**
 * Part of migration tests from EJB3 testsuite to AS7. This test tests issue ejbthree1807. The problem was that
 * StatefulTreeCache removal task couldn't handle already removed beans.
 * 
 * @author Brian Stansberry, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemovalTaskFailureUnitTestCase {
    private static final Logger log = Logger.getLogger(RemovalTaskFailureUnitTestCase.class);
    private static final String ARCHIVE_NAME = "cache-removal-test";
    private static final String PROPERTIES_FILE = "cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties";
    
    private static RemoteEJBDirectory context;
    
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;
    
    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(RemoveRejecter.class, RemoveRejecterBean.class);
        log.info(jar.toString(true));
        return jar;
    }
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        context = new RemoteEJBDirectory(ARCHIVE_NAME);
    }
    
    @Test
    @InSequence(-2)
    public void startServers() {
        log.info("Starting server: " + CONTAINER_1);
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
    }

    @Test
    @InSequence(1)
    public void testRemovalTaskFailureHandling(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client) throws Exception {
        DMRUtil.setPassivationIdleTimeout(client.getControllerClient());
        
        ContextSelector<EJBClientContext> previousSelector = EJBClientContextSelector.setup(PROPERTIES_FILE);
        
        try {
            RemoveRejecter allow = (RemoveRejecter) context.lookupStateful(RemoveRejecterBean.class, RemoveRejecter.class);
            Thread.sleep(WAIT_FOR_PASSIVATION_MS);
            Assert.assertNotNull(allow);
            allow.setRejectRemove(false);
            RemoveRejecter reject = (RemoveRejecter) context.lookupStateful(RemoveRejecterBean.class, RemoveRejecter.class);
            Thread.sleep(WAIT_FOR_PASSIVATION_MS);
            Assert.assertNotNull(reject);
            allow.setRejectRemove(true);
            
           Thread.sleep(100 * 1000);
    
            /*
             * MBeanServerConnection server = getServer(); ObjectName testerName = new
             * ObjectName("jboss.j2ee:jar=ejbthree1807.jar,name=RemoveRejecterBean,service=EJB3"); int cacheSize =
             * (Integer)server.getAttribute(testerName, "CacheSize"); Assert.assertEquals(2, cacheSize); int totalSize =
             * (Integer)server.getAttribute(testerName, "TotalSize"); Assert.assertEquals(2, totalSize); // Allow removal to run
             * twice Thread.sleep(3 * 1000); cacheSize = (Integer)server.getAttribute(testerName, "CacheSize");
             * Assert.assertEquals(0, cacheSize); totalSize = (Integer)server.getAttribute(testerName, "TotalSize");
             * Assert.assertEquals(0, totalSize); int removeCount = (Integer)server.getAttribute(testerName, "RemoveCount");
             * Assert.assertEquals(2, removeCount);
             */
        } finally {
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }
    }
    
    @Test
    @InSequence(100)
    public void stopAndClean(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client) throws Exception {
        DMRUtil.unsetIdleTimeoutPassivationAttribute(client.getControllerClient());
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
    }
}
