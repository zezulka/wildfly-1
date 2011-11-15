package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;


@Stateful
@SecurityDomain("ejb3-tests")
public class SFSBLifecycleCallback {
    
    private static Logger log = Logger.getLogger(SFSBLifecycleCallback.class);
    
    @Resource
    private SessionContext sessContext;
    
    //TODO: @PreDestroy test
    @PostConstruct
    public void init() throws Exception {
        // on Stateful bean is permitted to call getCallerPrincipal on @PostConstruct
        TestResultsSingleton results = (TestResultsSingleton) sessContext.lookup("java:module/" + TestResultsSingleton.class.getSimpleName());
        
        Principal princ = sessContext.getCallerPrincipal();
        results.addSfsb(princ);
        log.info(SFSBLifecycleCallback.class.getSimpleName() + " @PostConstruct called");
    }
    
    public String get() {
        log.info("stateful get() principal: " + sessContext.getCallerPrincipal());
        return "stateful";
    }
}
