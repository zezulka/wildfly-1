/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Inc., and individual contributors as indicated
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

package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.MarshalledObject;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.ejb.Handle;
import javax.ejb.RemoveException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.UserTransaction;

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
 * Tests that corba name lookups work from inside the AS itself
 * 
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class IIOPNamingInContainerTestCase {
    private static final Logger log = Logger.getLogger(IIOPNamingInContainerTestCase.class);

    @Deployment(name = "test")
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar").addPackage(IIOPNamingInContainerTestCase.class.getPackage());
    }

    @Deployment(name = "test2")
    public static Archive<?> descriptorOverrideDeploy() {
        return ShrinkWrap.create(JavaArchive.class, "test2.jar")
                .addClasses(IIOPNamingHome.class, IIOPRemote.class, IIOPNamingBean.class)
                .addAsManifestResource("ejb/iiop/jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    @Test
    public void testIIOPNamingInvocation() throws NamingException, RemoteException {

        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    @Test
    public void testStatefulIIOPNamingInvocation() throws NamingException, RemoteException, RemoveException {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPStatefulNamingBean");
        final IIOPStatefulNamingHome object = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(iiopObj,
                IIOPStatefulNamingHome.class);
        final IIOPStatefulRemote result = object.create(10);
        Assert.assertEquals(11, result.increment());
        Assert.assertEquals(12, result.increment());
        result.remove();
        try {
            result.increment();
            Assert.fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {

        }
    }

    /**
     * <p>
     * Tests the lookup of a bean that used the jboss-ejb3.xml deployment descriptor to override the COSNaming binding. So,
     * insteand of looking for the standard test2/IIOPNamingBean context we will look for the configured
     * bean/custom/name/IIOPNamingBean context.
     * </p>
     * 
     * @throws NamingException if an error occurs while looking up the bean.
     * @throws RemoteException if an error occurs while invoking the remote bean.
     */
    @Test
    public void testIIOPNamingInvocationWithDDOverride() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:localhost:3528#bean/custom/name/IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    /**
     * Part of AS5->AS7 testsuite migration [JBQA-5275] (ejb3/iiop/unit/IiopRemoteUnitTestCase).
     */
    @Test
    public void testTxPropagation() throws Exception {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        Object obj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPTxTesterBean");
        IIOPTxTesterHome sessionHome = (IIOPTxTesterHome) PortableRemoteObject.narrow(obj, IIOPTxTesterHome.class);
        IIOPTxTester session = sessionHome.create();
        Assert.assertNotNull(session);
        UserTransaction tx;
        try {
            tx = (UserTransaction) PortableRemoteObject.narrow(context.lookup("corbaname:iiop:localhost:3528#UserTransaction"),
                    UserTransaction.class);
        } catch (NameNotFoundException e) {
            log.warn("Corba Transaction Service is not installed (not available with Arjuna, only with JBossTS)");
            return;
        }
        tx.begin();
        try {
            session.txMandatoryMethod();
        } finally {
            tx.rollback();
        }
        // If it doesn't throw an exception everything is fine.
    }

    @Test
    public void testTxRequired() throws Exception {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        Object obj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPTxTesterBean");
        IIOPTxTesterHome sessionHome = (IIOPTxTesterHome) PortableRemoteObject.narrow(obj, IIOPTxTesterHome.class);
        IIOPTxTester session = sessionHome.create();
        Assert.assertNotNull(session);
        try {
            session.txMandatoryMethod();
            Assert.fail("Expected an exception");
        } catch (javax.transaction.TransactionRequiredException e) {
            // ok
        } catch (Exception e) {
            Assert.fail("Expected an TransactionRequiredException");
        }
    }

    @Test
    public void testIsIdentical() throws Exception {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        Object obj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPStatefulNamingBean");
        IIOPStatefulNamingHome home = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(obj, IIOPStatefulNamingHome.class);
        IIOPStatefulRemote session = home.create(111);
        Handle h = session.getHandle();
        MarshalledObject mo = new MarshalledObject(h);
        Handle h2 = (Handle) mo.get();
        Object o = h2.getEJBObject();
        IIOPStatefulRemote session2 = (IIOPStatefulRemote) PortableRemoteObject.narrow(o, IIOPStatefulRemote.class);
        Assert.assertTrue(session.isIdentical(session2));
    }

    @Test
    public void testRemoveByHandle() throws Exception {
        final Properties prope = new Properties();
        final InitialContext context = new InitialContext(prope);
        Object obj = context.lookup("corbaname:iiop:localhost:3528#test/IIOPStatefulNamingBean");
        IIOPStatefulNamingHome home = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(obj, IIOPStatefulNamingHome.class);
        IIOPStatefulRemote session = home.create(112);
        session.increment();
        home.remove(session.getHandle());
        try {
            session.increment();
            Assert.fail("should throw an exception");
        } catch (Exception e) {
            log.info(e);
            // ok
        }
    }
}
