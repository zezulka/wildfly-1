/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.management.api.expression;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;

import junit.framework.Assert;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Validation of the system property substitution for expressions handling. Test for AS7-6120.
 * Validation of expression settings to parameters globally could be found at domain module in test: ExpressionSupportSmokeTestCase
 * 
 * @author <a href="ochaloup@jboss.com">Ondrej Chaloupka</a>
 */
@RunWith(Arquillian.class)
public class ValidateExpressionSubstitution2TestCase {
    private static final Logger log = Logger.getLogger(ValidateExpressionSubstitution2TestCase.class);
    
    private static boolean isSystemPropertyCreated = false;
    
    private static final String TEST_PROPERTY_NAME = "testsuite.expression.property";
    private static final String EXPRESSION_EVALUATION_PROPERTY = "test.exp";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String SYSTEM_PROPERTY_VALUE = "setBySystemProperty";
    private static final String JBOSS_PROPERTY_VALUE = "setByJBossProperty";
        
    @ContainerResource 
    private static ManagementClient managementClient;
    
    @Before
    public void setup() throws Exception {
        if(!isSystemPropertyCreated) {
            String value = "${" + EXPRESSION_EVALUATION_PROPERTY + ":" + DEFAULT_VALUE + "}";
            setProperty(TEST_PROPERTY_NAME, value, managementClient);
            isSystemPropertyCreated = true;
        }
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        removeProperty(TEST_PROPERTY_NAME, managementClient);
    }
    
    private static void setProperty(String name, String value, ManagementClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, ADD);
        modelNode.get(VALUE).set(value);
        try {
            ModelNode result = client.getControllerClient().execute(modelNode);
            log.info("Added property " + name + ", result: " + result);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    private static void removeProperty(String name, ManagementClient client) {
        ModelNode modelNode = createOpNode("system-property=" + name, REMOVE);
        try {
            ModelNode result = client.getControllerClient().execute(modelNode);
            log.info("Removing property " + name + ", result: " + result);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
       
    private String getProperty(String name) {
        ModelNode modelNode = createOpNode("system-property=" + name, READ_ATTRIBUTE_OPERATION);
        modelNode.get(NAME).set(VALUE);
        try {
            ModelNode result = managementClient.getControllerClient().execute(modelNode);
            Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
            ModelNode resolvedResult = result.resolve();
            log.info("Resolved property " + TEST_PROPERTY_NAME + ": " + resolvedResult);
            Assert.assertEquals(SUCCESS, resolvedResult.get(OUTCOME).asString());
            return resolvedResult.get("result").asString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    /**
     * No property set - the default value should be found.
     */
    @Test
    public void testDefault() throws IOException {
        Assert.assertEquals("No system property set expecting default value", DEFAULT_VALUE, getProperty(TEST_PROPERTY_NAME));
    }
    
    /**
     * Property set via System.setProperty()
     */
    @Test
    public void testProperty() {
        
        System.setProperty(EXPRESSION_EVALUATION_PROPERTY, SYSTEM_PROPERTY_VALUE);
        Assert.assertEquals("System property set via System.setProperty()", SYSTEM_PROPERTY_VALUE, getProperty(TEST_PROPERTY_NAME));
        System.clearProperty(EXPRESSION_EVALUATION_PROPERTY);
        Assert.assertEquals("System property removed expected default value", DEFAULT_VALUE, getProperty(TEST_PROPERTY_NAME));
    }
    
    /**
     * Property set via management client and /system-property=property-name:add(value=value)
     */
    @Test
    public void testJbossProperty() {
        setProperty(EXPRESSION_EVALUATION_PROPERTY, JBOSS_PROPERTY_VALUE, managementClient);
        Assert.assertEquals("System property set via JBoss property definition", JBOSS_PROPERTY_VALUE, getProperty(TEST_PROPERTY_NAME));
        removeProperty(EXPRESSION_EVALUATION_PROPERTY, managementClient);
        Assert.assertEquals("JBoss property removed expected default value", DEFAULT_VALUE, getProperty(TEST_PROPERTY_NAME));
    }
    
    /**
     * Property set via management client and System.setProperty()
     */
    @Test
    public void testJbossPropertyAndSystemProperty() {
        setProperty(EXPRESSION_EVALUATION_PROPERTY, JBOSS_PROPERTY_VALUE, managementClient);
        System.setProperty(EXPRESSION_EVALUATION_PROPERTY, SYSTEM_PROPERTY_VALUE);
        
        Assert.assertEquals("System property set via JBoss property definition", JBOSS_PROPERTY_VALUE, getProperty(TEST_PROPERTY_NAME));
        removeProperty(EXPRESSION_EVALUATION_PROPERTY, managementClient);
        Assert.assertEquals("JBoss property removed expected default value", DEFAULT_VALUE, getProperty(TEST_PROPERTY_NAME));
        
        removeProperty(EXPRESSION_EVALUATION_PROPERTY, managementClient);
        removeProperty(EXPRESSION_EVALUATION_PROPERTY, managementClient);
    }
}
