package org.jboss.as.test.integration.ejb.transaction.utils;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.logging.Logger;

public final class TestLookupUtil {
    private static final Logger log = Logger.getLogger(TestLookupUtil.class);

    private TestLookupUtil() {
        // not instance here
    }

    public static <T> T lookupModule(InitialContext initCtx, Class<T> beanType) throws NamingException {
        String lookupString = String.format("java:module/%s!%s", beanType.getSimpleName(), beanType.getName());
        log.debug("looking for: " + lookupString);
        return beanType.cast(initCtx.lookup(lookupString));
    }

    public static <T> T lookupIIOP(String serverHost, int iiopPort, Class<T> homeClass, Class<?> beanClass) throws NamingException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.iiop.naming:org.jboss.naming.client");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + serverHost +":" + iiopPort + "/JBoss/Naming/root");
        final InitialContext context = new InitialContext(prope);
        final Object ejbHome = context.lookup(beanClass.getSimpleName());
        return homeClass.cast(PortableRemoteObject.narrow(ejbHome, homeClass));
    }
}
