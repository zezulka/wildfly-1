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

package org.jboss.as.test.xts.newxts.base;

import org.jboss.logging.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a simple Set collection.
 *
 * @author paul.robinson@redhat.com
 */
public class MockSet {
    private static final Logger log = Logger.getLogger(MockSet.class);

    private static final Set<String> set = new HashSet<String>();

    /**
     * Add a value to the set
     * 
     * @param item Item to add to the set.
     * @throws AlreadyInSetException if the item is already in the set.
     */
    public static void add(String item) throws TestApplicationException {
        synchronized (set) {

            if (set.contains(item)) {
                throw new TestApplicationException("Item '" + item + "' is already in the set.");
            }
            set.add(item);
        }
    }

    /**
     * Persist sufficient data, such that the add operation can be undone or made permanent when told to do so by a call to
     * commit or rollback.
     * 
     * As this is a mock implementation, the method does nothing and always returns true.
     * 
     * @return true if the SetManager is able to commit and the required state was persisted. False otherwise.
     */
    public static boolean prepare() {
        return true;
    }

    /**
     * Make the outcome of the add operation permanent.
     * 
     * As this is a mock implementation, the method does nothing.
     */
    public static void commit() {
        log.info("[SERVICE] Commit the backend resource (e.g. commit any changes to databases so that they are visible to others)");
    }

    /**
     * Undo any changes made by the add operation.
     * 
     * As this is a mock implementation, the method needs to be informed of how to undo the work of the add operation. Typically
     * resource managers will already know this information.
     * 
     * @param item The item to remove from the set in order to undo the effects of the add operation.
     */
    public static void rollback(String item) {
        log.info("Compensate the backend resource by removing '" + item
                + "' from the set (e.g. undo any changes to databases that were previously made visible to others)");
        synchronized (set) {

            set.remove(item);
        }

    }

    /**
     * Query the set to see if it contains a particular value.
     * 
     * @param value the value to check for.
     * @return true if the value was present, false otherwise.
     */
    public static boolean isInSet(String value) {
        return set.contains(value);
    }

    /**
     * Empty the set
     */
    public static void clear() {
        set.clear();
    }
}
