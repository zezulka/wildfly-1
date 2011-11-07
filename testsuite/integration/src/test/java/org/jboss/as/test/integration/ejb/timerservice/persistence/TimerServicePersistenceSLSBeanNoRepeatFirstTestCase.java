/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.timerservice.persistence;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Testcase for EJBTHREE-1926 https://jira.jboss.org/jira/browse/EJBTHREE-1926
 * The bug was caused by restoring the timers too early during the EJB3 container
 * startup (in the stateless/service container lockedStart()).
 * 
 * The fix involved restoring the timers after the containers have started
 * and are ready to accept invocations.
 * 
 * 
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class TimerServicePersistenceSLSBeanNoRepeatFirstTestCase {

    /**
     * must match between the two tests.
     */
    public static final String ARCHIVE_NAME = "testTimerServicePersistence.war";

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(TimerServicePersistenceSLSBeanNoRepeatFirstTestCase.class.getPackage());
        return war;

    }
    
    @AfterClass
    public static void waiting() throws Exception {
        // waiting between deployments
        Thread.sleep(1100);
        System.out.println("Waiting");
    }

    @Test
    public void createTimerService() throws NamingException {
        InitialContext ctx = new InitialContext();
        SimpleTimerSLSBeanNoRepeat bean = (SimpleTimerSLSBeanNoRepeat)ctx.lookup("java:module/" + SimpleTimerSLSBeanNoRepeat.class.getSimpleName());
        bean.createTimer(1000);
    }



}
