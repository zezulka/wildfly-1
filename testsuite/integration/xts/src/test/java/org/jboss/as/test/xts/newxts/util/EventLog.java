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

package org.jboss.as.test.xts.newxts.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public class EventLog implements Serializable {

    // Event logs for a name
    private static volatile Map<String, List<EventLogEvent>> dataUnavailableLog = new HashMap<String, List<EventLogEvent>>();
    private static volatile Map<String, List<EventLogEvent>> eventLog = new HashMap<String, List<EventLogEvent>>();
    
    private static final String UNKNOWN_EVENTLOG_NAME = "unknown";

    /**
     * Method checks whether the eventLogName exists in the datastore.
     * In case that it exists - do nothing.
     * In case that it does not exists - it creates the key with empty list of logged events.
     * 
     * @param eventLogName  name of key for events
     */
    public void foundEventLogName(String eventLogName) {
        getListToModify(eventLogName, eventLog);
    }
    
    public void addEvent(EventLogEvent event) {
        addEvent(UNKNOWN_EVENTLOG_NAME, event);
    }
       
    public void addEvent(String eventLogName, EventLogEvent event) {
        getListToModify(eventLogName, eventLog).add(event);
    }

    public void addDataUnavailable(EventLogEvent event) {
        addDataUnavailable(UNKNOWN_EVENTLOG_NAME, event);
    }
    
    public void addDataUnavailable(String eventLogName, EventLogEvent event) {
        getListToModify(eventLogName, dataUnavailableLog).add(event);
    }

    public List<EventLogEvent> getEventLog() {
        return getEventLog(UNKNOWN_EVENTLOG_NAME);
    }
    
    public List<EventLogEvent> getEventLog(String eventLogName) {
        return eventLog.get(eventLogName);
    }
    
    public List<EventLogEvent> getDataUnavailableLog() {
        return getDataUnavailableLog(UNKNOWN_EVENTLOG_NAME);
    }
    
    public List<EventLogEvent> getDataUnavailableLog(String eventLogName) {
        return dataUnavailableLog.get(eventLogName);
    }

    public void clear() {
        eventLog.clear();
        dataUnavailableLog.clear();
    }

    public static String asString(List<EventLogEvent> events) {
        String result = "";

        for (EventLogEvent logEvent : events) {
            result += logEvent.name() + ",";
        }
        return result;
    }
    
    // --- helper method
    private <T> List<T> getListToModify(String eventLogName, Map<String, List<T>> map) {
        if(map.containsKey(eventLogName)) {
            return map.get(eventLogName);
        } else {
            List<T> newList = new ArrayList<T>();
            map.put(eventLogName, newList);
            return newList;
        }
    }
}
