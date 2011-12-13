/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.ejb2.ejb3refer.reference.a;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for EJB3.0/EJB2.1 references
 * 
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@RunWith(Arquillian.class)
public class ReferenceTestCase {

    private static final Logger log = Logger.getLogger(ReferenceTestCase.class);

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "reference-ejb2-ejb3-a.jar")
           .addClasses(
                   HomedStatefulSession30Bean.class,
                   StatefulSession30.class,
                   StatefulSession30Home.class,
                   StatefulSession30RemoteBusiness.class,
                   ReferenceTestCase.class);
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testSession21() throws Exception {
       Assert.fail("failing");
    }

}
