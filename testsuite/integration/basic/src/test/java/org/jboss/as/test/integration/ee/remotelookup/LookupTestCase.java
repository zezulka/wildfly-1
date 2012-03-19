package org.jboss.as.test.integration.ee.remotelookup;

import static org.jboss.as.test.integration.common.jms.JMSOperationsProvider.getInstance;

import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(Arquillian.class)
@ServerSetup(LookupTestCase.TestCaseSetup.class)
public class LookupTestCase {
    
    private static String QUEUE_1 = "queue/sendQueueOne";
    // private static String QUEUE_2 = "queue/sendQueueTwo";
    
    static class TestCaseSetup implements ServerSetupTask {
        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations operations = getInstance(managementClient);
            operations.createJmsQueue(QUEUE_1, "java:jboss/" + QUEUE_1);
            // operations.createJmsQueue(QUEUE_2, "java:jboss/exported/" + QUEUE_2);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations operations = getInstance(managementClient);
            operations.removeJmsQueue(QUEUE_1);
            // operations.removeJmsQueue(QUEUE_2);
        }
    }
    
    @Deployment(name = "test")
    public static Archive<?> deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "deploy.jar");
        jar.addClass(StatelessBean.class);
        return jar;
    }

    @Test
    @OperateOnDeployment("test")
    @InSequence(1)
    public void test1() throws Exception {
        InitialContext ic = new InitialContext();
        ConnectionFactory qcf = (ConnectionFactory) ic.lookup("java:/RemoteConnectionFactory");
        ic.lookup("java:jboss/" + QUEUE_1);
        Connection conn = qcf.createConnection("guest", "guest");
        conn.close();
        ic.close();
    }
    
    @Test
    @RunAsClient
    @InSequence(2)
    public void test2() throws Exception {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        env.put(Context.PROVIDER_URL, "remote://127.0.0.1:4447");
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        Context ctx = new InitialContext(env);
        
        ctx.lookup("jms/RemoteConnectionFactory");
        ctx.close();
    }
}
