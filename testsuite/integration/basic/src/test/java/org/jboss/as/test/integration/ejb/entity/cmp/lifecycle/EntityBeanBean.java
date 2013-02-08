/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.entity.cmp.lifecycle;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;

import org.jboss.logging.Logger;

import javax.transaction.UserTransaction;

/**
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public abstract class  EntityBeanBean implements EntityBean, EntityBeanRemote {
    final static long serialVersionUID = 1L;
    
    private static final Logger log = Logger.getLogger(EntityBeanBean.class);
    
    private List<String> lifecycleEntries = new ArrayList<String>();
       
    private transient EntityContext ctx;

    public EntityBeanBean() {
    }

    public String ejbCreate(String id) throws CreateException {
        logLifecycleEvent("create");
        setId(id);
        return id;
    }

    public void ejbPostCreate(String id) {
        logLifecycleEvent("postCreate");
    }
       
    public void ejbActivate() {
        logLifecycleEvent("activate");
        
    }

    public void ejbPassivate() {
        logLifecycleEvent("passivate");
    }

    public void ejbLoad() {
        logLifecycleEvent("load");
    }

    public void ejbStore() {
        logLifecycleEvent("store");
    }

    public void ejbRemove() {
        logLifecycleEvent("remove");
    }
   
    public void setEntityContext(EntityContext ctx) {
        logLifecycleEvent("setEntityContext");
        this.ctx = ctx;
    }

    public void unsetEntityContext() {
        logLifecycleEvent("unsetEntityContext");
        this.ctx = null;
    }

    
    public abstract String getId();
    
    public abstract void setId(String id);
    
    // --- "Business" helper methods
    private String getObjectIdentityPrivate() {
        return ((Object) this).toString();
    }
    
    public String getObjectIdentity() throws RemoteException {
        if(ctx != null) {
            UserTransaction ut = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
            if(ut == null) {
                log.infof("Ctx: %s user transacton is null", ctx);
            } else {
                log.infof("Ctx: %s user transacton is %s - with hash code: %s", ctx, ut, ut.hashCode());
            }
        } 
        return this.getObjectIdentityPrivate();
    }
    
    // --- private helper methods
    private SingletonLoggerRemote getSingleton() {
        try {
            return (SingletonLoggerRemote) ctx.lookup("java:global/single/" + SingletonLogger.class.getSimpleName());
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }
    
    private String getLogMsg(String objectIdentity, String event) {
        String takenId = null;
        try {
            takenId = this.getId();
        } catch (Exception e) {
            // ignore this - just for logging purposes is the id taken
        }
        return String.format("%s objectIdentity: %s id: %s event: %s", EntityBeanBean.class.getSimpleName(), objectIdentity, takenId, event);
    }
    
    /**
     * Putting lifecycle event info to singleton
     * @param event  event name to be logged
     */
    private void logLifecycleEvent(String event) {
        SingletonLoggerRemote loggingSingleton = getSingleton();
        
        try {
            String pk = (String) ctx.getPrimaryKey();
            log.info("Primary key found as: " + pk);
            if(loggingSingleton != null) {
                loggingSingleton.logEntry(pk, event);
            }
        } catch (Exception e) {
            // ignore this
            log.info("Primary key was not got. Exception message: " + e.getMessage());
        }
        

        String objectIdentity = getObjectIdentityPrivate();
        lifecycleEntries.add(event);
        log.info(getLogMsg(objectIdentity, event));
        
        if(loggingSingleton != null) {
            for(String logEntry: lifecycleEntries){ 
              loggingSingleton.logEntry(objectIdentity, logEntry);
            }
            lifecycleEntries.clear();
        }
    }
}
