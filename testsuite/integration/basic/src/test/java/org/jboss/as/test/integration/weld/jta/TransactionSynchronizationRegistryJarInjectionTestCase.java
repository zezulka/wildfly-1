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

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class)
                .addPackage(TransactionSynchronizationRegistryJarInjectionTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return jar;
    }

    @Test
    public void testTransactionSynchronizationRegistryIsInjected() {
        Assert.assertTrue(cdiBean.isTransactionSynchronizationRegistryInjected());
    }
}
