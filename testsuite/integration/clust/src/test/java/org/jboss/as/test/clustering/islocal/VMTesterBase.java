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

package org.jboss.as.test.clustering.islocal;

import java.rmi.dgc.VMID;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author Brian Stansberry
 */
public class VMTesterBase {
    private static final Logger log = Logger.getLogger(VMTesterBase.class);

    private final VMID creatorVMID = VMTester.VMID;

    public VMID getVMID() {
        log.debug("Ignore; just a stack trace", new Exception("Ignore; just a stack trace"));
        return VMTester.VMID;
    }

    public VMID getCreatorVMID() {
        return creatorVMID;
    }

    public VMID getVMIDFromRemoteLookup(String jndiURL, String port, String name) throws NamingException {
        final Properties props = new Properties();
        // setup the ejb: namespace URL factory
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        // props.setProperty("jnp.disableDiscovery", "true");
        InitialContext ctx = new InitialContext(props);
        
        VMTester tester = (VMTester) ctx.lookup(name);
        return tester.getVMID();
    }

    public VMID getVMIDFromRemote(VMTester remote) {
        return remote.getVMID();
    }

}
