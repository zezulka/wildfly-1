package org.jboss.as.test.integration.ejb.remote.contextselector;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.logging.Logger;
import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@SecurityDomain("other")
public class StatelessBean implements StatelessBeanRemote {
    private static final Logger log = Logger.getLogger(StatelessBean.class);
    public static final String HELLO_STRING = "Hello";
    
    @Resource
    SessionContext context;
    
    @Override
    @RolesAllowed({"Role1", "Role2"})
    public String sayWhoHello() {
        String who = context.getCallerPrincipal().getName();
        log.info(StatelessBean.HELLO_STRING + " " + who);
        return StatelessBean.HELLO_STRING + " " + who;
    }
}
