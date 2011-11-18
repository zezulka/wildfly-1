package org.jboss.as.test.integration.ejb.security.callerprincipal.a;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;


@Stateless
@SecurityDomain("ejb3-tests")
public class SLSBLifecycleCallbackB {
    
    private static Logger log = Logger.getLogger(SLSBLifecycleCallbackB.class);
    
    @Resource
    private SessionContext sessContext;
    
    //TODO: @PreDestroy test
    @PostConstruct
    public void init() throws Exception {
        // on Stateless bean is not permitted to call getCallerPrincipal on @PostConstruct
        TestResultsSingleton results = (TestResultsSingleton) sessContext.lookup("java:module/" + TestResultsSingleton.class.getSimpleName());
        
        Principal princ = sessContext.getCallerPrincipal();
        results.addSlsb(princ);
        log.info(SLSBLifecycleCallbackB.class.getSimpleName() + " @PostConstruct called");
    }
    
    public String get() {
        return "stateless";
    }
}
