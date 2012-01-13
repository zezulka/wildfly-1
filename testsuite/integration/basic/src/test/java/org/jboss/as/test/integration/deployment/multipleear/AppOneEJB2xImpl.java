/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, JBoss Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.deployment.multipleear;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * FirstEJB2xImpl
 * 
 * @author Jaikiran Pai
 */
public class AppOneEJB2xImpl implements SessionBean, AppOneEJB2xRemote {

    public void ejbCreate() throws CreateException, RemoteException {

    }

    public void ejbActivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    public void ejbPassivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    public void ejbRemove() throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        // TODO Auto-generated method stub

    }

    public void doNothing() throws RemoteException {
        // do nothing

    }

    public EJBHome getEJBHome() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public Handle getHandle() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getPrimaryKey() throws RemoteException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isIdentical(EJBObject ejbo) throws RemoteException {
        // TODO Auto-generated method stub
        return false;
    }

    public void remove() throws RemoteException, RemoveException {
        // TODO Auto-generated method stub

    }

}
