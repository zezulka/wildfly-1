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

package org.jboss.as.test.clustering.islocal;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is testing of java.rmi.dgc.VMID. It's part of migration of tests from prior testsuites [JBQA-5855].
 * 
 * @author Brian Stansberry, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SimpleIsLocalInterceptorUnitTestCase extends InvokeLocalTestBase {
    private static Logger log = Logger.getLogger(SimpleIsLocalInterceptorUnitTestCase.class);

    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;
    
    // redefinition of archive name to parent class has this info as well
    static {
        ARCHIVE_NAME = "clusteredsession-local"; 
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
        final JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        ejb.addPackage(SimpleIsLocalInterceptorUnitTestCase.class.getPackage());
        ejb.addAsManifestResource(SimpleIsLocalInterceptorUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ejb.addAsManifestResource(SimpleIsLocalInterceptorUnitTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");        
        log.info(ejb.toString(true));
        return ejb;
    }
    
    @Test
    @InSequence(-1)
    public void startServers() {
        log.info("Starting server: " + CONTAINER_1);
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        log.info("Starting server: " + CONTAINER_2);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
    }

    /**
     * Setting URLs and ports which we can use for connection to active nodes
     */
    @Test
    @InSequence(0)
    public void settingRemoteUrlConnections(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2) {
        namingURLs[0][0] = client1.getRemoteEjbURL().getHost();
        namingURLs[0][1] = Integer.toString(client1.getRemoteEjbURL().getPort());
        namingURLs[1][0] = client2.getRemoteEjbURL().getHost();
        namingURLs[1][1] = Integer.toString(client2.getRemoteEjbURL().getPort());        
    }

    @Test
    @InSequence(1)
    public void testClusteredStatefulStaysLocal() throws Exception {       
        String jndiName = createJndiName("ClusteredStateful", VMTester.class, true);
        stayLocalTest(jndiName, true, true);
    }

    @Test
    @InSequence(1)
    public void testClusteredStatelessStaysLocal() throws Exception {
        String jndiName = createJndiName("ClusteredStateless", VMTester.class, false);
        stayLocalTest(jndiName, true, true);
    }

    @Test
    @InSequence(1)
    public void testNonClusteredStatefulGoesRemote() throws Exception {
        String jndiName = createJndiName("NonClusteredStateful", VMTester.class, true);
        stayLocalTest(jndiName, false, false);
    }

    @Test
    @InSequence(1)
    public void testNonClusteredStatelessGoesRemote() throws Exception {
        String jndiName = createJndiName("NonClusteredStateless", VMTester.class, false);
        stayLocalTest(jndiName, false, false);
    }

    @Test
    @InSequence(100)
    public void stopAndClean(
            @OperateOnDeployment(DEPLOYMENT_1) @ArquillianResource ManagementClient client1,
            @OperateOnDeployment(DEPLOYMENT_2) @ArquillianResource ManagementClient client2) throws Exception {
        log.info("Cleaning...");

        // returning to the previous context selector, @see {RemoteEJBClientDDBasedSFSBFailoverTestCase}
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }

        // unset & undeploy & stop
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);
    }
}
