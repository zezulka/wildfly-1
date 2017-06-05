package org.jboss.as.test.integration.weld.jta;

import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TransactionSynchronizationRegistryJarInjectionTestCase {

    @Inject
    CdiBean cdiBean;

    @Inject
    private EjbBean ejbBean;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
            .addClasses(CdiBean.class, TransactionSynchronizationRegistryJarInjectionTestCase.class, EjbBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return jar;
    }

    @Test
    public void testTransactionSynchronizationRegistryIsInjected() {
        Assert.assertTrue("@Resource at ejb should work", ejbBean.isTransactionSynchronizationRegistryInjected());
        Assert.assertTrue("@Resource at cdi should work", cdiBean.isTransactionSynchronizationRegistryInjected());
    }

    @Test
    public void testTransactionManagerIsInjected() {
        Assert.assertTrue(cdiBean.isTransactionManagerInjected());
    }
}
