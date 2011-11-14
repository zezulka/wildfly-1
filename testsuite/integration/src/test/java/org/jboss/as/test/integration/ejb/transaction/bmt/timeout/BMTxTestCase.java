/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.bmt.timeout;

import javax.naming.InitialContext;

import org.junit.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * Test migrated from EJB3 testsuite [JBQA-5451] - from test ejbthree2238
 * 
 * Tests that a bean marked for Bean Managed Transactions isn't picked up by Container Managed Transaction intereceptor.
 * 
 * @see https://issues.jboss.org/browse/EJBTHREE-2238 for the complete bug details 
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class BMTxTestCase {
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "bmt-timeout-test.jar");
        jar.addPackage(BMTxTestCase.class.getPackage());
        return jar;
    }

    /**
     * Tests that the BMT bean isn't invoked in a CMT context.
     * 
     * @see https://issues.jboss.org/browse/EJBTHREE-2238
     * @throws Exception
     */
    @Test
    public void testBMTxManagement() throws Exception {
        TxActivity bmtBean = (TxActivity) new InitialContext().lookup("java:module/" + BMTBean.class.getSimpleName());
        bmtBean.doTxStuff();
    }
}
