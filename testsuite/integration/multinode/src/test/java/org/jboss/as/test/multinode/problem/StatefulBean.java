package org.jboss.as.test.multinode.problem;
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

import java.util.Properties;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateful;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
@Stateful
@Remote(StatefulRemote.class)
@Local(StatefulLocal.class)
public class StatefulBean implements StatefulRemote, StatefulLocal {
    private static final Logger log = Logger.getLogger(StatefulBean.class);

    private static int homeMethodCount = 0;

    private InitialContext getInitialContext() throws NamingException {
        final Properties props = new Properties();
        // setup the ejb: namespace URL factory
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        return new InitialContext(props);
    }

    public int call() throws Exception {
        InitialContext jndiContext = getInitialContext();
        log.info("StatefulBean call()" + jndiContext);
        ++homeMethodCount;
        
        StatefulRemote stateful = (StatefulRemote) jndiContext.lookup("ejb:/" + TestTestCase.SERVER_DEPLOYMENT + "//"
                + StatefulBean.class.getSimpleName() + "!" + StatefulRemote.class.getName() + "?stateful");
        return stateful.homeMethod();
    }

    public int homeMethod() throws Exception {
        log.info("Before adding ++ is homeMethodCount: " + homeMethodCount);
        ++homeMethodCount;
        log.info("*** homeMethod called " + homeMethodCount);
        return homeMethodCount;
    }

    public void ejbCreate() throws java.rmi.RemoteException, javax.ejb.CreateException {
        log.debug("Creating method for home interface...");
    }
}
