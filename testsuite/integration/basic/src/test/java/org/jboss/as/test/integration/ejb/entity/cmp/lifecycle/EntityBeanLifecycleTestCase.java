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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.EJBClientTransactionContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

/**
 * Testing entity bean lifecycle as it's defined at 10.1.3  Instance Life Cycle (p.293)
 * 
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EntityBeanLifecycleTestCase {
    private static final Logger log = Logger.getLogger(EntityBeanLifecycleTestCase.class);
    
    private static final String CREATE = "create";
    private static final String POST_CREATE = "postCreate";
    private static final String ACTIVATE = "activate";
    private static final String STORE = "store";
    private static final String LOAD = "load";
    
    private static final String SINGLETON_ARCHIVE_NAME = "single";
    private static final String ENTITY_BEAN_ARCHIVE_NAME = "eb";
    private static final String ENTITY_BEAN_ID = "testing_id";
    
    private static String nodeName;
    private static List<String> storeLoadFilterList;

    @ArquillianResource
    private InitialContext initialContext;
   

    @Deployment(name="single", order = 0)
    public static Archive<?> deploymentSingleton()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SINGLETON_ARCHIVE_NAME + ".jar")
                .addClass(SingletonLogger.class)
                .addClass(SingletonLoggerRemote.class);
        log.info(jar.toString(true));
        return jar;
    }

    @Deployment(name="eb", order = 1)
    public static Archive<?> deploymentEntityBean()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ENTITY_BEAN_ARCHIVE_NAME + ".jar")
                .addClass(EntityBeanBean.class)
                .addClass(EntityBeanRemoteHome.class)
                .addClass(EntityBeanRemote.class)
                .addAsManifestResource(EntityBeanLifecycleTestCase.class.getPackage(), "MANIFEST.MF-entitybean", "MANIFEST.MF")
                .addAsManifestResource(EntityBeanLifecycleTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    private SingletonLoggerRemote getResultsSingleton() throws NamingException {
        return (SingletonLoggerRemote) initialContext.lookup("ejb:/" + SINGLETON_ARCHIVE_NAME + "//" + SingletonLogger.class.getSimpleName() + "!" + SingletonLoggerRemote.class.getName());
    }
    
    private EntityBeanRemoteHome getEntityBeanHome() throws NamingException {
        return (EntityBeanRemoteHome) initialContext.lookup("ejb:/" + ENTITY_BEAN_ARCHIVE_NAME + "//EntityBeanLifecycle!" + EntityBeanRemoteHome.class.getName());
    }

    /**
     * Create and setup the remoting connection
     *
     * @throws Exception
     */
    @BeforeClass
    public static void beforeTestClass() throws Exception {
        // the node name that the test methods can use
        nodeName = EJBManagementUtil.getNodeName();
        log.info("Using node name " + nodeName);
        // filter store and load string
        storeLoadFilterList = new ArrayList<String>();
        storeLoadFilterList.add(LOAD);
        storeLoadFilterList.add(STORE);
    }
    
    /**
     * Needed for transaction ctx to be correctly used and propagated to AS.
     */
    @Before
    public void beforeTest() throws Exception {
        // Create and setup the EJB client context backed by the remoting receiver
        final EJBClientTransactionContext localUserTxContext = EJBClientTransactionContext.createLocal();
        // set the tx context
        EJBClientTransactionContext.setGlobalContext(localUserTxContext);
    }
    
    @Test
    public void testEBActivate() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        // taking entity bean with the same id from pool
        EntityBeanRemote entityBeanFound = home.findByPrimaryKey(ENTITY_BEAN_ID);
        
        // jvm object identity taken
        String ebCreatedIdentity = entityBeanCreated.getObjectIdentity();
        String ebFoundIdentity = entityBeanFound.getObjectIdentity();
        
        if(ebCreatedIdentity.equals(ebFoundIdentity)) {
            Assert.fail("Entity identities are the same for the created and found entity bean. Not possible to test. Please change the testcase.");
        }
        
        log.info("entity bean created object identity: " + ebCreatedIdentity);
        log.info("entity bean found object identity: " + ebFoundIdentity);
        
        List<String> createdIdentityLog = results.getLog(ebCreatedIdentity);
        List<String> foundIdentityLog = results.getLog(ebFoundIdentity);
        
        showSingletonLog(createdIdentityLog);
        showSingletonLog(foundIdentityLog);
        
        entityBeanCreated.remove();
        entityBeanCreated.unsetEntityContext();
    }

    @Test
    public void testJustCreate() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        
        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        ut.commit();
        
        try {
            // showing info by logging it
            List<String> singletonLog = results.getLog(ENTITY_BEAN_ID);
            log.info("testJustCreate: after creation");
            showSingletonLog(singletonLog);
            
            List<String> filteredSingletonLog = filterList(singletonLog, storeLoadFilterList);
            // no business method called - no lifecycle method has to be called
            Assert.assertTrue("No business method called - the activate method on CMB can't be run", !filteredSingletonLog.contains(ACTIVATE));
            Assert.assertTrue("No business method called - the postCreate method on CMB can't be run", !filteredSingletonLog.contains(POST_CREATE));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }
    
    
    @Test
    public void testCreateAndWithBusinessMethod() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        
        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        
        try {    
            // calling business method in another transaction
            entityBeanCreated.getObjectIdentity();
            ut.commit();
            
            // showing info by logging it
            List<String> singletonLog = results.getLog(ENTITY_BEAN_ID);
            log.info("testCreateAndWithBussinessMethod: after creation with business method called");
            showSingletonLog(singletonLog);
            
            List<String> filteredSingletonLog = filterList(singletonLog, storeLoadFilterList);
            // the entity bean was created we suppose to get postCreate
            Assert.assertTrue("Business method called after entity creation - postCreate has to be called", filteredSingletonLog.contains(POST_CREATE));
            Assert.assertTrue("Business method called after entitty creation - no activate method expected", !filteredSingletonLog.contains(ACTIVATE));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }
    
    @Test
    public void testTransactionOnCreate() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        
        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        ut.rollback();
        
        try {
            ut.begin();
            home.findByPrimaryKey(ENTITY_BEAN_ID);
            ut.commit();
        } catch (Exception e) {
            // this is expected because the bean should not exist
            log.info("OK - the entity bean does not exist - creation was rollbacked");
            return;            
        }
        // entity exists - removing
        entityBeanCreated.remove();
        Assert.fail("The transaction was rollbacked. The entity should not exist.");
    }
    
    @Test
    public void testFind() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        
        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        ut.commit();
        
        // clearing singleton log - we are interested in what the findByPK will do
        results.clearLog();
        log.info("Log cleared...");
        
        try {
            ut.begin();
            home.findByPrimaryKey(ENTITY_BEAN_ID);
            ut.commit();
            
            // showing info by logging it
            List<String> singletonLog = results.getLog(ENTITY_BEAN_ID);
            log.info("testFind: after find");
            showSingletonLog(singletonLog);
            
            List<String> filteredSingletonLog = filterList(singletonLog, storeLoadFilterList);
            // no business method called - no lifecycle method has to be called
            Assert.assertTrue(!filteredSingletonLog.contains(ACTIVATE));
            Assert.assertTrue(!filteredSingletonLog.contains(POST_CREATE));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }
    
    @Test
    public void testFindAndCallBusinessMethod() throws Exception {
        SingletonLoggerRemote results = getResultsSingleton();
        results.clearLog();
        
        final UserTransaction ut = EJBClient.getUserTransaction(nodeName);
        ut.begin();
        
        // getting home interface
        EntityBeanRemoteHome home = getEntityBeanHome();
        // creating entity
        EntityBeanRemote entityBeanCreated = home.create(ENTITY_BEAN_ID);
        ut.commit();
        
        // clearing singleton log - we are interested in what the findByPK will do
        results.clearLog();
        log.info("Log cleared...");
        
        try {
            ut.begin();
            EntityBeanRemote entityBeanFound = home.findByPrimaryKey(ENTITY_BEAN_ID);
            String objectIdentity = entityBeanFound.getObjectIdentity();
            ut.commit();
            
            // showing info by logging it
            List<String> singletonLog = results.getLog(ENTITY_BEAN_ID);
            log.info("testFindAndCallBusinessMethod: after find");
            showSingletonLog(singletonLog);
            
            List<String> filteredSingletonLog = filterList(singletonLog, storeLoadFilterList);
            // business method called - supposing activate
            Assert.assertTrue(filteredSingletonLog.contains(ACTIVATE));
            Assert.assertTrue(!filteredSingletonLog.contains(POST_CREATE));
        } finally {
            // removing entity at the end
            entityBeanCreated.remove();
        }
    }
    
    
    // -------------------------- HELPER METHODS ---------------------
    /**
     * Just logging string in list one by one.
     */
    private void showSingletonLog(List<String> logEntries) {
        int i = 1;
        for (String logEntry: logEntries) {
            log.infof("[%s] %s", i++, logEntry);
        }
    }
    
    /**
     * It will remove from @param listToFilter all string contained in @param filterItems.
     * @return filtered list (empty list when input parameters are null)
     */
    private List<String> filterList(List<String> listToFilter, List<String> filterItems) {
        List<String> returnList = new ArrayList<String>();
        if(listToFilter == null || filterItems == null) {
            return returnList;
        }
        
        for (String item: listToFilter) {
            if(!filterItems.contains(item)) {
                returnList.add(item);
            }
        }
        return returnList;
    }
}
