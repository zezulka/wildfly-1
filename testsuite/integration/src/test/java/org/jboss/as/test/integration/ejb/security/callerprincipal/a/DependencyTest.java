package org.jboss.as.test.integration.ejb.security.callerprincipal.a;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.List;

import java.util.Properties;
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
import javax.naming.Context;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.validation.constraints.AssertTrue;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing
 */
@RunWith(Arquillian.class)
public class DependencyTest extends SecurityTest {
    
    private static Logger log = Logger.getLogger(DependencyTest.class);
    
    /* @ArquillianResource
    InitialContext ctx; */
    
    @ArquillianResource
    Deployer deployer;
    
    @Deployment(managed=true, testable = false, name = "single", order = 1)
    public static Archive<?> deploymentSingleton()  {
        // FIXME hack to get things prepared before the deployment happens and it has happend in first deploy!!
        // @see  org.jboss.as.test.integration.ejb.security.AuthenticationTestCase
        try {
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }
        
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar")
                .addClass(TestResultsSingleton.class)
                .addAsManifestResource(DependencyTest.class.getPackage(), "MANIFEST.MF-single", "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }
    
    @Deployment(managed=false, testable = false, name = "slsb", order = 100)
    public static Archive<?> deploymentSlsb()  {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "slsb.jar")
                .addClass(SLSBLifecycleCallbackB.class)
                .addAsManifestResource(DependencyTest.class.getPackage(), "MANIFEST.MF-slsb", "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }
    
    
    @Deployment(managed = true, testable = true, name="test", order = 2)
    public static Archive<?> deploymentTest()  {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClass(Base64.class)
                .addClass(SecurityTest.class)
                .addClass(DependencyTest.class)                
                .addAsResource("ejb3/security/users.properties", "users.properties")
                .addAsResource("ejb3/security/roles.properties", "roles.properties")
                .addAsManifestResource(DependencyTest.class.getPackage(), "MANIFEST.MF-test", "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }
       
    @AfterClass 
    public static void clearUp() throws Exception {
        removeSecurityDomain();
    }
    
    @Test @OperateOnDeployment("test")
    public void test(@ArquillianResource InitialContext ctx) throws NamingException {
        deployer.deploy("slsb");
        log.info("HIIIIIIIIII");
        
        /*Properties jndiProps = new Properties();
        jndiProps.put("java.naming.factory.initial","org.jboss.as.naming.InitialContextFactory");
        jndiProps.put("java.naming.factory.url.pkgs","org.jboss.naming:org.jnp.interfaces");
        jndiProps.put("java.naming.provider.url","localhost:1099");
        InitialContext ctx = new InitialContext(jndiProps);*/
        
        // SLSBLifecycleCallbackB a = (SLSBLifecycleCallbackB) ctx.lookup("java:global/slsb/" + SLSBLifecycleCallbackB.class.getSimpleName() + "!" + SLSBLifecycleCallbackB.class.getName());
        //SLSBLifecycleCallbackB a = (SLSBLifecycleCallbackB) ctx.lookup("java:global/slsb/" + SLSBLifecycleCallbackB.class.getSimpleName());
        Context a = (Context) ctx.lookup("java:jboss");
        log.info("Testing..." + ctx + ", is something in a: " + a);
        Assert.assertTrue(true);
    }
}
