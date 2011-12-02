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

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

public abstract class SimpleLocalBean implements EntityBean {
    private static final long serialVersionUID = 1L;
    private transient EntityContext ctx;

    public SimpleLocalBean() {
    }

    public abstract String getId();

    public abstract void setId(String id);
    
    public abstract String getAbc();

    public abstract void setAbc(String id);

    public String ejbCreate(String id) throws CreateException {
        setId(id);
        return id;
    }
    
    // home
    public void ejbPostCreate(String id) {
    }

    public void setEntityContext(EntityContext ctx) {
        this.ctx = ctx;
    }

    public void unsetEntityContext() {
        this.ctx = null;
    }

    // entity bean
    @Override
    public void ejbRemove() throws RemoveException, EJBException, RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ejbLoad() throws EJBException, RemoteException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ejbStore() throws EJBException, RemoteException {
        // TODO Auto-generated method stub
        
    }
}
