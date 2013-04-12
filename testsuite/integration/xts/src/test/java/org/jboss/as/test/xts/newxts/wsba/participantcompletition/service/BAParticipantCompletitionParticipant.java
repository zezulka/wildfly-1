/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. 
 * See the copyright.txt in the distribution for a full listing 
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 * 
 * (C) 2005-2006,
 * @author JBoss Inc.
 */
package org.jboss.as.test.xts.newxts.wsba.participantcompletition.service;

import com.arjuna.wst.BusinessAgreementWithParticipantCompletionParticipant;
import com.arjuna.wst.FaultedException;
import com.arjuna.wst.SystemException;
import com.arjuna.wst.WrongStateException;
import com.arjuna.wst11.ConfirmCompletedParticipant;

import org.jboss.as.test.xts.newxts.base.MockSet;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.util.EventLogEvent;
import org.jboss.as.test.xts.newxts.util.ServiceCommand;
import org.jboss.logging.Logger;

import java.io.Serializable;

/**
 * An adapter class that exposes the SetManager as a WS-BA participant using the 'Participant Completion' protocol.
 * 
 * The Set Service only allows a single item to be added to the set in any given transaction. So, this means it can complete at
 * the end of the addValueToSet call, rather than having to wait for the coordinator to tell it to do so. Hence it uses a
 * participant which implements the 'participant completion' protocol.
 * 
 * @author Paul Robinson (paul.robinson@redhat.com)
 */
public class BAParticipantCompletitionParticipant 
        implements BusinessAgreementWithParticipantCompletionParticipant, ConfirmCompletedParticipant, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(BAParticipantCompletitionParticipant.class);
    
    private String value;
    // Service command which define behaving of the participant
    private ServiceCommand[] serviceCommands;
    // Where to log participant activity
    private EventLog eventLog;
   

    /**
     * Participant instances are related to business method calls in a one to one manner.
     * 
     * @param value the value to remove from the set during compensation
     */
    public BAParticipantCompletitionParticipant(ServiceCommand[] serviceCommands, EventLog eventLog, String value) {
        this.value = value;
        this.eventLog = eventLog;
        this.serviceCommands = serviceCommands;
    }

    public String status() {
        return null;
    }

    /**
     * The transaction has completed successfully. The participant previously informed the coordinator that it was ready to
     * complete.
     * 
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */

    public void close() throws WrongStateException, SystemException {
        eventLog.addEvent(EventLogEvent.CLOSE);
        // The participant knows that this BA is now finished and can throw away any temporary state
        // nothing to do here as the item has already been added to the set
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant close() - logged: " + EventLogEvent.CLOSE);
    }

    /**
     * The transaction has canceled, and the participant should undo any work. The participant cannot have informed the
     * coordinator that it has completed.
     * 
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException never in this implementation.
     */

    public void cancel() throws WrongStateException, SystemException {
        eventLog.addEvent(EventLogEvent.CANCEL);
        // The participant should compensate any work done within this BA
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant cancel() - logged: " + EventLogEvent.CANCEL);
        // Compensate work
        MockSet.rollback(value);
    }

    /**
     * The transaction has cancelled. The participant previously informed the coordinator that it had finished work but could
     * compensate later if required, and it is now requested to do so.
     * 
     * @throws com.arjuna.wst.WrongStateException never in this implementation.
     * @throws com.arjuna.wst.SystemException if unable to perform the compensating transaction.
     */

    public void compensate() throws FaultedException, WrongStateException, SystemException {
        eventLog.addEvent(EventLogEvent.COMPENSATE);
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant compensate() - logged: " + EventLogEvent.COMPENSATE);
        // Compensate work done by the service
        MockSet.rollback(value);
    }

    @Deprecated
    public void unknown() throws SystemException {
        eventLog.addEvent(EventLogEvent.UNKNOWN);
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant unknown() - logged: " + EventLogEvent.UNKNOWN);
    }

    public void error() throws SystemException {
        eventLog.addEvent(EventLogEvent.ERROR);
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant error() - logged: " + EventLogEvent.ERROR);
        // Compensate work done by the service
        MockSet.rollback(value);
    }

    /**
     * method called to perform commit or rollback of prepared changes to the underlying manager state after the participant
     * recovery record has been written
     * 
     * @param confirmed true if the log record has been written and changes should be rolled forward and false if it has not
     *        been written and changes should be rolled back
     */
    public void confirmCompleted(boolean confirmed) {
        log.info("[BA PARTICIPANT COMPL SERVICE] Participant confirmCompleted(" + Boolean.toString(confirmed) + ")");
        if (confirmed) {
            // This tells the participant that compensation information has been logged and that it is safe to commit any changes
            eventLog.addEvent(EventLogEvent.CONFIRM_COMPLETED);
            log.info("[BA PARTICIPANT COMPL SERVICE] Participant confirmCompleted(true) - logged: " + EventLogEvent.CONFIRM_COMPLETED);
            MockSet.commit();
        } else {
            MockSet.rollback(value);
        }
    }
}
