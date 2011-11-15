package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;


@Stateless
@SecurityDomain("ejb3-tests")
public class SLSBLifecycleCallback {
    
    private static Logger log = Logger.getLogger(SLSBLifecycleCallback.class);
    
    @Resource
    private SessionContext sessContext;
    
    //TODO: @PreDestroy test
    @PostConstruct
    public void init() throws Exception {
        // on Stateless bean is not permitted to call getCallerPrincipal on @PostConstruct
        TestResultsSingleton results = (TestResultsSingleton) sessContext.lookup("java:module/" + TestResultsSingleton.class.getSimpleName());
        
        results.setPrivateInfo("Hi setter!");
        results.publicInfo = "Hi public variable!";
        
        Principal princ = sessContext.getCallerPrincipal();
        results.addSlsb(princ);
        log.info(SLSBLifecycleCallback.class.getSimpleName() + " @PostConstruct called");
    }
    
    public String get() {
        return "stateless";
    }
}
