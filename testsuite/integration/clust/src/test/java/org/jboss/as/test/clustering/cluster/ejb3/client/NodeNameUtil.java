package org.jboss.as.test.clustering.cluster.ejb3.client;

import org.jboss.logging.Logger;


public class NodeNameUtil {
    private static final Logger log = Logger.getLogger(NodeNameUtil.class);
    public static String getNodeName() {
        String nodename = System.getProperty("jboss.node.name");
        log.info("I'm server: " + nodename);
        return nodename;
    }
}
