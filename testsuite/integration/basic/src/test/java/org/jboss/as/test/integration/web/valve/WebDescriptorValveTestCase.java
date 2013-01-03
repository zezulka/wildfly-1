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

package org.jboss.as.test.integration.web.valve;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * This class tests a global valve.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebDescriptorValveTestCase.ValveSetup.class)
public class WebDescriptorValveTestCase {
    private static Logger log = Logger.getLogger(WebDescriptorValveTestCase.class);
    
    private static final String modulename = "org.jboss.testvalve";
    private static final String classname = TestValve.class.getName();
    private static final String baseModulePath = "/../modules/" + modulename.replace(".", "/") + "/main";
    private static final String jarName = "testvalve.jar";
    private static final String VALVE_NAME = "testvalve";
    private static final String PARAM_NAME = "testparam";
    private static final String WEB_PARAM_VALUE = "webdescriptor";
    private static final String GLOBAL_PARAM_VALUE = "global";

    static class ValveSetup implements ServerSetupTask {       
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.createValveModule(managementClient, modulename, baseModulePath, jarName);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.removeValve(managementClient, VALVE_NAME);
        }
    }

    @Deployment(name = "valve")
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-descriptor-valve-test.war");
        war.addClasses(HelloServlet.class);
        war.addAsWebInfResource(WebDescriptorValveTestCase.class.getPackage(),"jboss-web.xml", "jboss-web.xml");
        war.addAsManifestResource(WebDescriptorValveTestCase.class.getPackage(),"MANIFEST.MF", "MANIFEST.MF");
        return war;
    }
    
    @Test
    @InSequence(1)
    public void testWebDescriptor(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {        
        log.debug("Testing url " + url + " against one valve defined in jboss-web.xml descriptor");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals(WEB_PARAM_VALUE, valveHeaders[0].getValue());
    }

    @Test
    @InSequence(2)
    public void testValveOne(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        Map<String,String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, GLOBAL_PARAM_VALUE);
        ValveUtil.addValve(client, VALVE_NAME, modulename, classname, params);
        ValveUtil.reload(client);
        
        log.debug("Testing url " + url + " against two valves - one defined in web descriptor other is defined globally in server configuration");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There were two valves defined", 2, valveHeaders.length);
        assertEquals(GLOBAL_PARAM_VALUE, valveHeaders[0].getValue());
        assertEquals(WEB_PARAM_VALUE, valveHeaders[1].getValue());
    }
}