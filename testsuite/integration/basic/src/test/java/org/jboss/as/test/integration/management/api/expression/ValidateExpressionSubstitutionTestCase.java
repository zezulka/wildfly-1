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

import javax.naming.InitialContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
@RunAsClient
public class ValidateExpressionSubstitutionTestCase extends ContainerResourceMgmtTestBase {
    private static final Logger log = Logger.getLogger(ValidateExpressionSubstitutionTestCase.class);
    private static final String ARCHIVE_NAME = "expression-substitution-test";
    private static final String TEST_PROPERTY_NAME = "testsuite.expression.property";
    private static final String EXPRESSION_EVALUATION_PROPERTY = "test.exp";
    
    @ArquillianResource
    private InitialContext ctx;
    
    @Deployment(name = ARCHIVE_NAME)
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(RemotePropertyHelper.class, RemotePropertyHelperBean.class);
        // jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        log.info(jar.toString(true));
        return jar;
    }
    
    private RemotePropertyHelper getHelper() {
        RemotePropertyHelper helper;
        try {
            helper = (RemotePropertyHelper) ctx.lookup("ejb:/" + ARCHIVE_NAME + "//" + RemotePropertyHelperBean.class.getSimpleName() + 
                "!" + RemotePropertyHelper.class.getName());
        } catch ( Exception e) {
            throw new RuntimeException(e);
        }
        return helper;
    }
    
    private ModelNode executeOp(ModelNode op) {
        try {
            return executeOperation(op);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private String resolveProperty(String propertyName) {
        ModelNode modelNode = createOpNode("system-property=" + propertyName, READ_ATTRIBUTE_OPERATION);
        modelNode.get(NAME).set(VALUE);
        return executeOp(modelNode).asString();
    }
    
    @Before
    public void prepareEvaluationProperty() {
        ModelNode modelNode = createOpNode("system-property=" + TEST_PROPERTY_NAME, ADD);
        modelNode.get("value").set("${" + EXPRESSION_EVALUATION_PROPERTY + ":defaultValue}");
        executeOp(modelNode);
    }
    
    @Test
    public void testPropertySet() {
        getHelper().setProperty(EXPRESSION_EVALUATION_PROPERTY, "propertySetValue");
    }

    @Override
    protected ModelControllerClient getModelControllerClient() {
        // TODO Auto-generated method stub
        return null;
    }
}
