/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import javax.transaction.SystemException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import org.wildfly.transaction.client.LocalTransactionContext;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinationManager;

/**
 * This handler serves for integeration of Narayana txbridge and
 * wilfdly transaction client.<br>
 * The only purpose is to import global transaction managed by client
 * to Narayana for txbrige implemente there would be aware of it
 * and {@link SubordinationManager} could work with it.
 *
 * @author <a href="mailto:ochaloup@redhat.com">Ondrej Chaloupka</a>
 */
public class XTSImportGlobalTxnInboundBridgeHandler implements Handler {

    @Override
    public boolean handleMessage(final MessageContext messageContext) {
        // pull in any XTS transaction
        try {
            LocalTransactionContext.getCurrent().importProviderTransaction();
        } catch (SystemException e) {
            throw new IllegalStateException("not working"); // TODO: we need to put this to XtsAsLogger
        }
        // messageContext.
        // context.setTransaction(ContextTransactionManager.getInstance().suspend());
        return true;
    }

    @Override
    public boolean handleFault(final MessageContext messageContext) {
        return true;
    }

    @Override
    public void close(final MessageContext messageContext) {
    }
}
