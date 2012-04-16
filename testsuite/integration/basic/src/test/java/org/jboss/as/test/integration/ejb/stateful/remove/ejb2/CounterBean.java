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

package org.jboss.as.test.integration.ejb.stateful.remove.ejb2;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.RemoteHome;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Stateful
@Remote(CounterRemote.class)
@RemoteHome(CounterRemoteHome.class)
public class CounterBean implements SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CounterBean.class);
    
    private int count;
    
    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
        // Creating method for home interface...
    }
    
    public int increment() {
        return ++count;
    }

    public int decrement() {
        return --count;
    }

    public int getCount() {
        return count;
    }
    
    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        int count = CounterSingleton.destroyCounter.incrementAndGet();
        log.info("ejbRemove called [" + count + "] ...");
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
        
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        
    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {
        
    }
}
