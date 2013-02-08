/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.entity.cmp.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
@Singleton
@Startup
public class SingletonLogger implements SingletonLoggerRemote {
    private static final Map<String, List<String>> logContainer = new HashMap<String, List<String>>();

    @Override
    public void logEntry(String key, String entry) {
        Boolean isContain = logContainer.containsKey(key);
        if(isContain) {
            // log.infof("Adding key: %s and entry: %s", key, entry);
            logContainer.get(key).add(entry);
        } else {
            // log.infof("Creating key: %s and entry: %s", key, entry);
            List<String> containerValue = new ArrayList<String>();
            containerValue.add(entry);
            logContainer.put(key, containerValue);
        }
    }

    @Override
    public List<String> getLog(String entry) {
        List<String> ret = logContainer.get(entry);
        if(ret == null) {
            ret = new ArrayList<String>();
        }
        return ret;
    }

    @Override
    public void clearLog() {
        logContainer.clear();
    }
}
