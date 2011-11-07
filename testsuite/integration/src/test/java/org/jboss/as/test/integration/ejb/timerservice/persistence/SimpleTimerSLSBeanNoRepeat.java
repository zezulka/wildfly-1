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

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

import org.jboss.logging.Logger;


import java.util.Date;

/**
 * Migrating from EJB3 testsuite [JBQA-5451]
 * 
 * Testcase for EJBTHREE-1926 https://jira.jboss.org/jira/browse/EJBTHREE-1926
 * The bug was caused by restoring the timers too early during the EJB3 container
 * startup (in the stateless/service container lockedStart()).
 *  * The fix involved restoring the timers after the containers have started
 * and are ready to accept invocations.
 *  
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@Stateless
public class SimpleTimerSLSBeanNoRepeat {

    private static final Logger log = Logger.getLogger(SimpleTimerSLSBeanNoRepeat.class);
    
    public static boolean timerServiceCalled = false;

    @Resource
    private TimerService timerService;
   
    /**
    * @param delay  in miliseconds
     */
    public void createTimer(long delay)
    {
       Date scheduledTime = new Date(new Date().getTime() + delay);
       javax.ejb.Timer timer = this.timerService.createTimer(delay, "This is a timer which was scheduled to fire at "
             + scheduledTime);
       log.info("Timer  " + timer + " scheduled to fire once at " + timer.getNextTimeout());

    }
    
    @Timeout
    public void timeout(Timer timer) {
        log.info("Received timeout at " + new Date(System.currentTimeMillis()) + " from timer " + timer
                + " with info: " + timer.getInfo());
        timerServiceCalled = true;
    }
}
