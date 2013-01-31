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

package org.jboss.as.test.integration.management.api.expression;

import java.io.File;

import javax.ejb.EJB;
import javax.inject.Inject;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The expression substitution test which run the evaluation of expression in bean in deployed in container. 
 * 
 * @author <a href="ochaloup@jboss.com">Ondrej Chaloupka</a> 
 */
@RunWith(Arquillian.class)
public class ExpressionSubstitutionInContainerTestCase {
    private static final Logger log = Logger.getLogger(ExpressionSubstitutionInContainerTestCase.class);
    
    private static final String ARCHIVE_NAME = "expression-substitution-test";
    private static boolean isSystemPropertyCreated = false;
    
    private static final String TEST_PROP_NAME = "qa.test.property";
    private static final String TEST_PROP_DEFAULT_VALUE = "defaultValue";
    private static final String TEST_EXPRESSION_PROP_NAME = "qa.test.exp";
    private static final String TEST_EXPRESSION_PROP_VALUE = "expression.value";
    
    @EJB(mappedName = "java:global/expression-substitution-test/StatelessBean")
    private static IStatelessBean bean;
    
    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME + ".war");
        war.addClasses(ExpressionTestManagementService.class, Utils.class, ModelUtil.class, 
                ServletTest.class, IStatelessBean.class, StatelessBean.class);
        
        war.addAsManifestResource(new StringAsset(ExpressionTestManagementService.class.getName()),
                "services/org.jboss.msc.service.ServiceActivator");
        war.addAsWebInfResource(new StringAsset(ExpressionTestManagementService.class.getName()),
                "classes/META-INF/services/org.jboss.msc.service.ServiceActivator"); // https://issues.jboss.org/browse/AS7-5172
        war.addAsManifestResource(new StringAsset(
                "Manifest-Version: 1.0\n" +
                "Class-Path: \n" +  // there has to be a spacer - otherwise you meet "java.io.IOException: invalid header field"     
                "Dependencies: org.jboss.msc,org.jboss.as.controller-client,org.jboss.as.controller,org.jboss.as.server, org.jboss.dmr\n"),
                "MANIFEST.MF");
        war.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        File testPackage = new File("/tmp", ARCHIVE_NAME + ".war");
        war.as(ZipExporter.class).exportTo(testPackage, true);
        
        return war;
    }
    
    
    @Before
    public void setup() throws Exception {
        if(!isSystemPropertyCreated) {
            bean.addJBossProperty(TEST_EXPRESSION_PROP_NAME, TEST_EXPRESSION_PROP_VALUE);
            bean.addJBossProperty(TEST_PROP_NAME, "${" + TEST_EXPRESSION_PROP_NAME + ":" + TEST_PROP_DEFAULT_VALUE + "}");
            isSystemPropertyCreated = true;
        }
    }
        
    @AfterClass
    public static void tearDown() throws Exception {
        bean.removeJBossProperty(TEST_EXPRESSION_PROP_NAME);
        bean.removeJBossProperty(TEST_PROP_NAME);
    }
    
    @Test
    public void test() {
        String result = bean.getJBossProperty(TEST_EXPRESSION_PROP_NAME);
        log.infof("JBoss property %s was resolved to %s", TEST_EXPRESSION_PROP_NAME, result);
        Assert.assertEquals(TEST_EXPRESSION_PROP_VALUE, result);
        
        
        result = bean.getJBossProperty(TEST_PROP_NAME);
        log.infof("JBoss property %s was resolved to %s", TEST_PROP_NAME, result);
        Assert.assertEquals(TEST_EXPRESSION_PROP_VALUE, result);
        

        result = bean.getJBossProperty(TEST_EXPRESSION_PROP_NAME);
        log.infof("System property %s has value %s", TEST_EXPRESSION_PROP_NAME, result);
        Assert.assertEquals(TEST_EXPRESSION_PROP_VALUE, result);
        
        result = bean.getJBossProperty(TEST_PROP_NAME);
        log.infof("System property %s has value %s", TEST_PROP_NAME, result);
        Assert.assertEquals(TEST_EXPRESSION_PROP_VALUE, result);
    }
}
