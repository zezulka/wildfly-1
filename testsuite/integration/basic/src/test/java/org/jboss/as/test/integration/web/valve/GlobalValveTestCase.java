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
 * @author Jean-Frederic Clere
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(GlobalValveTestCase.ValveSetup.class)
public class GlobalValveTestCase {
    private static Logger log = Logger.getLogger(GlobalValveTestCase.class);
    
    private static final String modulename = "org.jboss.testvalve";
    private static final String classname = TestValve.class.getName();
    private static final String baseModulePath = "/../modules/" + modulename.replace(".", "/") + "/main";
    private static final String jarName = "testvalve.jar";
    private static final String VALVE_NAME_1 = "testvalve1";
    private static final String VALVE_NAME_2 = "testvalve2";
    private static final String PARAM_NAME = "testparam";
    /** the default value is hardcoded in {@link TestValve} */
    private static final String DEFAULT_PARAM_VALUE = "DEFAULT_VALUE";

    static class ValveSetup implements ServerSetupTask {       
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.createValveModule(managementClient, modulename, baseModulePath, jarName);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            ValveUtil.removeValve(managementClient, VALVE_NAME_1);
            ValveUtil.removeValve(managementClient, VALVE_NAME_2);
            ValveUtil.reload(managementClient);
        }
    }

    @Deployment(name = "valve")
    public static WebArchive Hello() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "global-valve-test.war");
        war.addClasses(HelloServlet.class);
        return war;
    }

    @Test
    @InSequence(1)
    public void testValveOne(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        ValveUtil.addValve(client, VALVE_NAME_1, modulename, classname, null);
        ValveUtil.reload(client);
        
        log.debug("Testing url " + url + " against one global valve named " + VALVE_NAME_1);
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There was one valve defined - it's missing now", 1, valveHeaders.length);
        assertEquals("One valve with not defined param expecting default param value", DEFAULT_PARAM_VALUE, valveHeaders[0].getValue());
        log.info("testValveOne - OK");
    }

    @Test
    @InSequence(2)
    public void testValveTwo(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {
        Map<String,String> params = new HashMap<String, String>();
        params.put(PARAM_NAME, VALVE_NAME_2); //as param of valve defining its name
        ValveUtil.addValve(client, VALVE_NAME_2, modulename, classname, params);
        ValveUtil.reload(client);
        
        log.debug("Testing url " + url + " against two valves named " + VALVE_NAME_1 + " and " + VALVE_NAME_2);
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There were two global valves defined - it's missing now", 2, valveHeaders.length);
        log.info("First valve header: " + valveHeaders[0].getValue());
        assertEquals("First valve is defined without parameter - default value expected", DEFAULT_PARAM_VALUE, valveHeaders[0].getValue());
        log.info("Second valve header: " + valveHeaders[1].getValue());
        assertEquals("Second valve has parameter which is expected to be returned", VALVE_NAME_2, valveHeaders[1].getValue());
        log.info("testValveTwo - OK");
    }
    
    @Test
    @InSequence(3)
    public void testValveDisabled(@ArquillianResource URL url, @ArquillianResource ManagementClient client) throws Exception {       
        ValveUtil.activateValve(client, VALVE_NAME_1, false);
        ValveUtil.reload(client);
        
        log.debug("Testing url " + url + " against one global valve named " + VALVE_NAME_2 + ". The second one "+ VALVE_NAME_1 +" is disabled");
        Header[] valveHeaders = ValveUtil.hitValve(url);
        assertEquals("There is one active valve defined", 1, valveHeaders.length);
        assertEquals("Just second parametrized valve is active - defined param value is expected to be returned", VALVE_NAME_2, valveHeaders[0].getValue());
        log.info("testValveDisabled - OK");
    }
}