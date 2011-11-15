package org.jboss.as.test.integration.ejb.security.callerprincipal;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Singleton;
import javax.ejb.Startup;

@Singleton
@Startup
public class TestResultsSingleton {
    
    public String publicInfo;
    
    private String privateInfo;    
    
    private List<Principal> slsb = new ArrayList<Principal>();
    private List<Principal> sfsb = new ArrayList<Principal>();
    private List<Principal> mdb = new ArrayList<Principal>();
    private List<Principal> eb = new ArrayList<Principal>();
    
    public String getPrivateInfo() {
        return privateInfo;
    }

    public void setPrivateInfo(String privateInfo) {
        this.privateInfo = privateInfo;
    }

    public List<Principal> getSlsb() {
        return slsb;
    }

    public void addSlsb(Principal princ) {
        this.slsb.add(princ);
    }

    public List<Principal> getSfsb() {
        return sfsb;
    }

    public void addSfsb(Principal princ) {
        this.sfsb.add(princ);
    }

    public List<Principal> getMdb() {
        return mdb;
    }

    public void addMdb(Principal princ) {
        this.mdb.add(princ);
    }

    public List<Principal> getEb() {
        return eb;
    }

    public void addEb(Principal princ) {
        this.eb.add(princ);
    }
}
