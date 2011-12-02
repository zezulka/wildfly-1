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
package org.jboss.as.test.integration.ejb.entity.cmp.simple.remote;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class SimpleEntityBeanTestCase {
    private static Logger log = Logger.getLogger(SimpleEntityBeanTestCase.class);

    private static final String ARCHIVE_NAME = "simple-entity-bean.jar";
    
    @ArquillianResource
    InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addClasses(SimpleEntityBeanTestCase.class, SimpleLocal.class, SimpleLocalBean.class, SimpleLocalHome.class);
        jar.addAsManifestResource(SimpleEntityBeanTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }
    
    @Test
    public void test() throws Exception {
        SimpleLocal bean = setUpEjb();
        
        bean.setAbc("abc");
        String a = bean.getAbc();
        log.error("Gettign abc: " + a);
        
        tearDownEjb(bean);
    }

    private SimpleLocalHome getSimpleHome() {
        try {
            return (SimpleLocalHome) iniCtx
                    .lookup("java:module/SimpleEJB!" + SimpleLocalHome.class.getName());
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getSimpleHome: " + e.getMessage());
        }
        return null;
    }


    public SimpleLocal setUpEjb() throws Exception {
        SimpleLocalHome simpleHome = getSimpleHome();
        SimpleLocal simple = null;

        try {
            simple = simpleHome.findByPrimaryKey("simple");
        } catch (Exception e) {
        }

        if (simple == null) {
            simple = simpleHome.create("simple");
        }
        return simple;
    }

    public void tearDownEjb(SimpleLocal bean) throws Exception {
        bean.remove();
    }
}
