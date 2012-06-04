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

package org.jboss.as.test.integration.ejb.remote.contextselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientContextSelectorTestCase {
    private static final Logger log = Logger.getLogger(ClientContextSelectorTestCase.class);
    private static final String MODULE_NAME = "basic-client-context-selector";
    private static final String CLIENT_PROPERTIES = "jboss-ejb-client-security.properties";

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(StatelessBean.class.getPackage());
        log.info(ejbJar.toString(true));
        return ejbJar;
    }

    @Test
    public void test() throws Exception {
        ContextSelector<EJBClientContext> selector = null;
        InitialContext context = null;
        List<List<String>> users = new ArrayList<List<String>>();
        String user1[] = {"user1", "password1"};
        String user2[] = {"user2", "password2"};
        users.add(Arrays.asList(user1));
        users.add(Arrays.asList(user2));
        users.add(Arrays.asList(user1));
        
        for(List<String> user: users) {
            try {
                Properties properties = new Properties();
                properties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
                context = new InitialContext(properties);
                
                Properties prop = new Properties();
                // prop.put("remote.connection.default.connect.options.org.xnio.Options.SASL_DISALLOWED_MECHANISMS", "JBOSS-LOCAL-USER");
                prop.put("remote.connection.default.username", user.get(0));
                prop.put("remote.connection.default.password", user.get(1));
                log.infof("Using loging credentials: [user: %s, pass: %s]", user.get(0), user.get(1));
                selector = EJBClientContextSelector.setup(CLIENT_PROPERTIES, prop);
                
                
                StatelessBeanRemote bean = (StatelessBeanRemote) context.lookup("ejb:/" + MODULE_NAME +
                        "/" + StatelessBean.class.getSimpleName() + "!" + StatelessBeanRemote.class.getName());
                Assert.assertEquals(StatelessBean.HELLO_STRING + " " + user.get(0), bean.sayWhoHello());
            } finally {
                if (selector != null) {
                    EJBClientContext.setSelector(selector);
                    selector = null;
                }
                if(context != null) {
                    log.info("Closing context...");
                    context.close();
                    context = null;
                }
            }
        }
    }
}
