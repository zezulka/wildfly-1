/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.weld.jta.observe;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;

import org.jboss.logging.Logger;

public class TestPersistEntityListener {
    private static final Logger log = Logger.getLogger(TestPersistEntityListener.class);

    @Inject
    Event<PostPersistEvent> postPersistEvent;

    @PostPersist
    public void onEntityPostPerist(Object entity) {
        log.info("PostPersist entity was invoked for entity " + entity);
        PostPersistEvent eventPayload = new PostPersistEvent(entity.toString());
        postPersistEvent.fire(eventPayload);
    }

    @PrePersist
    public void onEntityPrePerist(Object entity) {
        log.info("PrePersist entity was invoked for entity " + entity);
    }
}
