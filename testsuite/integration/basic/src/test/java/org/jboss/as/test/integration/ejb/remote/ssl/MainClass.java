package org.jboss.as.test.integration.ejb.remote.ssl;

import java.net.URL;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.Assert;

public class MainClass {
    private static final String MODULE_NAME = "ssl-remote-ejb-client-test";
    
    public static void main(String[] args) throws Exception {
        
        System.setProperty("javax.net.ssl.trustStore", "/home/ochaloup/jboss/jboss-as-7/testsuite/integration/basic/target/test-classes/ejb/keystore/client.truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL resourcesUrl = tccl.getResource("");
        String resourcePath = resourcesUrl.getPath();
        System.out.println(resourcePath);
        
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        
        InitialContext ctx = new InitialContext(env);
        
        String searchFor = String.format("ejb:/%s//%s!%s", 
                MODULE_NAME, StatelessBean.class.getSimpleName(), StatelessBeanRemote.class.getName());
        System.out.println("Searching for: " + searchFor);
        StatelessBeanRemote bean = (StatelessBeanRemote) ctx.lookup(searchFor);
        if(StatelessBeanRemote.ANSWER.equals(bean.sayHello())) {
            System.out.println("Yupieee!");
        } else {
           System.out.println("Something is rotten in the state of Denmark");
        }
    }
}
