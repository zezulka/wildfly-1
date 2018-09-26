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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

public class TestEntityManipulatorBean {

    @PersistenceContext(unitName = "a-persistence-unit")
    EntityManager em;

    @Transactional
    public void createTestEntity(String name) {
        TestEntity newTestEntity = new TestEntity(name);
        em.persist(newTestEntity);
    }

    @Transactional
    public void createTestEntityFailure(String name) {
        TestEntity newTestEntity = new TestEntity(name);
        em.persist(newTestEntity);
        throw new RuntimeException("error happens, rollback expected");
    }

    public List<TestEntity> getAllEntities() {
        CriteriaQuery<TestEntity> query = em.getCriteriaBuilder().createQuery(TestEntity.class);
        Root<TestEntity> root = query.from(TestEntity.class);
        query.select(root);
        return em.createQuery(query).getResultList();
    }
}
