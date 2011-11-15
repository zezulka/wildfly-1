package org.jboss.as.test.integration.ejb.security.callerprincipal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.security.SecurityTest;
import org.jboss.logging.Logger;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.util.Base64;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The Bean Provider can invoke the getCallerPrincipal and isCallerInRole methods only
 * in the enterprise bean’s business methods as specified in Table 1 on page 94, Table 2 on page 103,
 * Table 4 on page 149, Table 5 on page 231, and Table 11 on page 303. If they are otherwise invoked
 * when no security context exists, they should throw the java.lang.IllegalStateException
 * runtime exception.
 * 
 * In case of no security context
 * Stateless - PostConstruct, PreDestroy
 * MDB - PostConstruct, PreDestroy
 * Entity Beans - ejbActivate, ebjPassivate
 * 
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class GetCallerPrincipalTestCase extends SecurityTest {
    
    private static Logger log = Logger.getLogger(GetCallerPrincipalTestCase.class);
    
    @ArquillianResource
    InitialContext ctx;
    
    @Deployment
    public static Archive<?> deployment()  {
        // FIXME hack to get things prepared before the deployment happens
        // @see  org.jboss.as.test.integration.ejb.security.AuthenticationTestCase
        try {
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "callerprincipal-test.jar")
                .addPackage(GetCallerPrincipalTestCase.class.getPackage())
                .addClass(Base64.class)
                .addClass(SecurityTest.class)
                .addAsResource("ejb3/security/users.properties", "users.properties")
                .addAsResource("ejb3/security/roles.properties", "roles.properties")
                .addAsManifestResource("web-secure-programmatic-login.war/MANIFEST.MF", "MANIFEST.MF");
                jar.addAsManifestResource(GetCallerPrincipalTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        log.info(jar.toString(true));
        return jar;
    }
       
    @AfterClass 
    public static void clearUp() throws Exception {
        removeSecurityDomain();
    }
    
    private TestResultsSingleton getResultsSingleton() throws NamingException {
        return (TestResultsSingleton) ctx.lookup("java:module/" + TestResultsSingleton.class.getSimpleName());
    }
    
    private SecurityClient login() throws Exception {
        final SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("user1", "password1");
        client.login();
        return client;
    }
    
    
    /*
     * Tests
     */
    @Test
    public void testUnauthenticatedNoSecurityDomain() throws Exception {
        try {
            SLSBWithoutSecurityDomain bean = (SLSBWithoutSecurityDomain) ctx.lookup("java:module/" + SLSBWithoutSecurityDomain.class.getSimpleName());
            final Principal principal = bean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                    principal);
            // TODO: where is 'anonymous' configured?
            assertEquals("anonymous", principal.getName());
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }
    
    @Test
    public void testStatelessPostConstruct() throws Exception {
        SecurityClient client = this.login();
        TestResultsSingleton results = this.getResultsSingleton();
        try {           
            SLSBLifecycleCallback bean = (SLSBLifecycleCallback) ctx.lookup("java:module/" + SLSBLifecycleCallback.class.getSimpleName());
            log.debug("Stateless bean returns: " + bean.get());

            // Testing how the @Singleton works
            log.info("Variable set via method: " + results.getPrivateInfo() + ", variable set directly: " + results.publicInfo);
            
            List<Principal> principals = results.getSlsb();
            log.info("Stateless principals:");
            for(Principal p: principals) {
                log.info(p);
            }
            // throw new Exception("Stateless exception");
        } finally {
            client.logout();
        }
    }
    
    @Test
    public void testStatefulPostConstruct() throws Exception {
        SecurityClient client = this.login();
        TestResultsSingleton results = this.getResultsSingleton();
        try {           
            SFSBLifecycleCallback bean = (SFSBLifecycleCallback) ctx.lookup("java:module/" + SFSBLifecycleCallback.class.getSimpleName());
            log.debug("Stateful bean returns: " + bean.get());
    
            List<Principal> principals = results.getSfsb();
            log.info("Stateful principals:");
            for(Principal p: principals) {
                log.info(p);
            }
            // throw new Exception("Statefull exception");
        } finally {
            client.logout();
        }
    }
    
    @Test
    public void testMDBPostConstruct() throws Exception {
        SecurityClient client = this.login();
        TestResultsSingleton results = this.getResultsSingleton();
        
        MessageProducer producer = null;
        MessageConsumer consumer = null;
        Connection conn = null;
        Session session = null;
        
        try {
            ConnectionFactory connFactory = (ConnectionFactory) ctx.lookup("java:/ConnectionFactory");
            conn = connFactory.createConnection();
            conn.start();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            TemporaryQueue replyQueue = session.createTemporaryQueue();
            
            TextMessage msg = session.createTextMessage("Hello world");
            msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
            msg.setJMSReplyTo(replyQueue);
            
            Queue queue = (Queue) ctx.lookup("java:/queue/test");
            producer = session.createProducer(queue);
            producer.send(msg);
            consumer = session.createConsumer(replyQueue);
            Message replyMsg = consumer.receive(5000);
            
            Object obj = ((ObjectMessage) replyMsg).getObject();
            log.debug("MDB message get: " + obj);
                        
            List<Principal> principals = results.getMdb();
            log.info("MDB principals:");
            for(Principal p: principals) {
                log.info(p);
            }
            // throw new Exception("MDB exception");
        } finally {
            if(consumer != null) {
                consumer.close();
            }
            if(producer!=null) {
                producer.close();
            }
            if(session!=null) {
                session.close();
            }
            if(conn!=null) {
                conn.stop();
            }
            client.logout();
        }
    }
    
    @Test
    public void testEBActivate() throws Exception {
        EntityBean eb = setUpEB();
        SecurityClient client = this.login();
        TestResultsSingleton results = this.getResultsSingleton();
        
        try {
            log.debug("Entity Bean ID: " + eb.getId());

            List<Principal> principals = results.getEb();
            log.info("Entity bean principals:");
            for(Principal p: principals) {
                log.info(p);
            }
            // throw new Exception("Entity bean exception");
        } finally {
            tearDownEB();
            client.logout();
        }
    }
    
    // Entity bean
    private EntityBean entityBean;
    
    private EntityBeanHome getEBHome() {
        try {
            return (EntityBeanHome) ctx.lookup("java:module/EntityBeanCallerPrincipal!" + EntityBeanHome.class.getName());
        } catch (Exception e) {
            log.error("failed", e);
            fail("Exception in getEBHome: " + e.getMessage());
        }
        return null;
    }
   
    private EntityBean setUpEB() throws Exception {
        EntityBeanHome ebHome = getEBHome();

        try {
            entityBean = ebHome.findByPrimaryKey("test");
        } catch (Exception e) {
        }

        if (entityBean == null) {
            entityBean = ebHome.create("test");
        }
        return entityBean;
    }

    private void tearDownEB() throws Exception {
        entityBean.remove();
    }
}
