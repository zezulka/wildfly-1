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

package org.jboss.as.test.integration.ejb.remote.ssl;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.io.File;
import java.net.URL;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 * Setup for ssl ejb remote connection.
 * Keystore created on basis of tutorial at https://community.jboss.org/wiki/SSLSetup.
 *
 * @author Ondrej Chaloupka
 */
public class SSLRealmSetupTask implements ServerSetupTask {
    private static final Logger log = Logger.getLogger(SSLRealmSetupTask.class);

    public static String PREVIOUS_SECURITY_REALM_NAME;
    public static String KEYSTORE_ABSOLUTE_PATH;
    public static final String SECURITY_REALM_NAME = "SSLRealm";
    public static final String KEYSTORE_RELATIVE_PATH = "ejb" + File.separator + "keystore";
    public static final String KEYSTORE_SERVER_FILENAME = "server.keystore";
    public static final String KEYSTORE_CLIENT_FILENAME = "client.keystore";
    public static final String KEYSTORE_PASSWORD = "123456";
    public static final String AUTHENTICATION_PROPERTIES_PATH = "application-users.properties";
    public static final String AUTHENTICATION_PROPERTIES_RELATIVE_TO = "jboss.server.config.dir";
    

    public static ModelNode getSecurityRealmsAddress() {
        ModelNode address = new ModelNode();
        address.add(CORE_SERVICE, MANAGEMENT);
        address.add(SECURITY_REALM, SECURITY_REALM_NAME);
        return address;
    }
    
    public static ModelNode getSecurityRealmsAddressSSLIdentity() {
        ModelNode address = getSecurityRealmsAddress();
        address.add(SERVER_IDENTITY, SSL);
        address.protect();
        return address;
    }
    
    public static ModelNode getSecurityRealmsAddressAuthentication() {
        ModelNode address = getSecurityRealmsAddress();
        address.add(AUTHENTICATION, PROPERTIES);
        address.protect();
        return address;
    }
    
    public static ModelNode getRemotingConnectorAddress() {
        ModelNode address = new ModelNode();
        address.add(SUBSYSTEM, "remoting");
        address.add("connector", "remoting-connector");
        address.protect();
        return address;
    }
    
    /**
     * Inspiration from {@link JcaMgmtBase}
     */
    public static void reload(final ManagementClient managementClient) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.info("Operation :reload executed with result " + result);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (managementClient.isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }

    /**
     * Associating the realm with remoting connector 
     * /subsystem=remoting/connector=remoting-connector:write-attribute(name=security-realm, value=SSLRealm)
     */
    public static void setRemotingConnectorRealm(final ManagementClient managementClient, final String realmName) throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(SSLRealmSetupTask.getRemotingConnectorAddress());
        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("security-realm");
        operation.get(VALUE).set(realmName);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.infof("Setting security realm %s to remoting connector with result %s", SSLRealmSetupTask.SECURITY_REALM_NAME, result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    }
   
    
    /**
    * <security-realm name="SSLRealm">
    *    <server-identities>
    *        <ssl>
    *            <keystore path="server.keystore" relative-to="[Thread.currentThread()]/[ejb/keystore]" keystore-password="123456"/>
    *        </ssl>
    *    </server-identities>
    *    <authentication>
    *        <properties path="application-users.properties" relative-to="jboss.server.config.dir"/>
    *    </authentication>
    * </security-realm>
     */
    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {       
        // Adding security realm
        ModelNode secRealmAddress = getSecurityRealmsAddress();
        secRealmAddress.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(secRealmAddress);
        operation.get(OP).set(ADD);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.infof("Adding security realm %s with result %s", SECURITY_REALM_NAME, result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        // Adding ssl attribute
        // /core-service=management/security-realm=SSLRealmOndra/server-identity=ssl:add(
        //   keystore-password=123456, keystore-relative-to=jboss.server.config.dir, keystore-path=keystore)
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL resourcesUrl = tccl.getResource("");
        String resourcePath = resourcesUrl.getPath();
        log.info("Path to resources is " + resourcePath);
        // String testAbsPath = new File(SSLEJBRemoteClientTestCase.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        operation = new ModelNode();
        operation.get(OP_ADDR).set(getSecurityRealmsAddressSSLIdentity());
        operation.get(OP).set(ADD);
        operation.get("keystore-password").set(KEYSTORE_PASSWORD);
        KEYSTORE_ABSOLUTE_PATH =  resourcePath + KEYSTORE_RELATIVE_PATH;
        // operation.get("keystore-relative-to").set(KEYSTORE_ABSOLUTE_PATH);
        operation.get("keystore-path").set(KEYSTORE_ABSOLUTE_PATH + File.separator + KEYSTORE_SERVER_FILENAME);
        result = managementClient.getControllerClient().execute(operation);
        log.infof("Setting server-identity ssl for realm %s (password %s, keystore path %s%s%s) with result %s", SECURITY_REALM_NAME,
                KEYSTORE_PASSWORD, KEYSTORE_ABSOLUTE_PATH, result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        reload(managementClient);
        Thread.sleep(10000);
          
        operation = new ModelNode();
        operation.get(OP_ADDR).set(getRemotingConnectorAddress());
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("security-realm");
        result = managementClient.getControllerClient().execute(operation);
        log.info("Reading attribute security-realm with result " + result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        PREVIOUS_SECURITY_REALM_NAME = result.get("result").asString();
        
        // Adding authentication attribute
        // authentication=properties:add(path=application-users.properties, relative-to=jboss.server.config.dir)
        operation = new ModelNode();
        operation.get(OP_ADDR).set(getSecurityRealmsAddressAuthentication());
        operation.get(OP).set(ADD);
        operation.get(PATH).set(AUTHENTICATION_PROPERTIES_PATH);
        operation.get(RELATIVE_TO).set(AUTHENTICATION_PROPERTIES_RELATIVE_TO);
        result = managementClient.getControllerClient().execute(operation);
        log.infof("Setting authentication of %s (path %s, relative to %s) with result %s", SECURITY_REALM_NAME,
                AUTHENTICATION_PROPERTIES_PATH, AUTHENTICATION_PROPERTIES_RELATIVE_TO, result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());        
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        // Removing security realm
        /*ModelNode secRealmAddress = getSecurityRealmsAddress();
        secRealmAddress.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(secRealmAddress);
        operation.get(OP).set(REMOVE);
        ModelNode result = managementClient.getControllerClient().execute(operation);
        log.infof("Removing security realm %s with result %s", SECURITY_REALM_NAME, result);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        
        reload(managementClient); */
    }
}