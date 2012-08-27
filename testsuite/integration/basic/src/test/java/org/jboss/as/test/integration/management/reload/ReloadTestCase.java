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

package org.jboss.as.test.integration.management.reload;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing how reload command work from MDR.
 * 
 * https://issues.jboss.org/browse/AS7-4185
 * https://issues.jboss.org/browse/ARQ-791
 * https://issues.jboss.org/browse/AS7-4185
 * https://issues.jboss.org/browse/AS7-5133
 * 
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ReloadTestCase {
    private static Logger log = Logger.getLogger(ReloadTestCase.class);
    
    @ContainerResource
    private ManagementClient client;

    @Deployment
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        jar.addClass(ReloadTestCase.class);
        return jar;
    }
    
    @Test
    public void test(@ArquillianResource ManagementClient client) throws Exception {
        // testsuite/integration/basic/src/test/java/org/jboss/as/test/integration/security/loginmodules/RunAsLoginModuleTestCase.java
        // op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "ejb3");
        address.add("strict-max-bean-instance-pool", "slsb-strict-max-pool");
        address.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("max-pool-size");
        operation.get(VALUE).set("2000");
        ModelNode result =  client.getControllerClient().execute(operation);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        log.info("After reloading: " + result);
        System.out.println("After reloading: " + result);
        
        operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        
        ModelNode op = new ModelNode();
        op.get(OP).set("reload");
        // op.get(ADMIN_ONLY).set(true);
        result =  client.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        log.info("After reloading: " + result);
        System.out.println("After reloading: " + result);
        
        Thread.sleep(5000);

        ModelNode rop = createOpNode(null, READ_ATTRIBUTE_OPERATION);
        rop.get(NAME).set("server-state");
        result = client.getControllerClient().execute(op);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        log.info("Server state: " + result);
        System.out.println("Server state: " + result);
        
        throw new IllegalAccessError();
    }
}
