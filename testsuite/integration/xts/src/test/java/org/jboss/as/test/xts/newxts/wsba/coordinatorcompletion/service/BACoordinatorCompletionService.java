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

package org.jboss.as.test.xts.newxts.wsba.coordinatorcompletion.service;

import org.jboss.as.test.xts.newxts.base.MockSet;
import org.jboss.as.test.xts.newxts.base.TestApplicationException;
import org.jboss.as.test.xts.newxts.util.EventLog;
import org.jboss.as.test.xts.newxts.util.ServiceCommand;
import org.jboss.logging.Logger;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.mw.wst11.BusinessActivityManager;
import com.arjuna.mw.wst11.BusinessActivityManagerFactory;
import com.arjuna.wst.SystemException;
import com.arjuna.wst11.BAParticipantManager;

import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.servlet.annotation.WebServlet;

import static org.jboss.as.test.xts.newxts.util.ServiceCommand.*;

@WebService(serviceName = "BACoordinatorCompletionService", 
        portName = "BACoordinatorCompletion", name = "BACoordinatorCompletion", 
        targetNamespace = "http://www.jboss.com/jbossas/test/xts/ba/coordinatorcompletion/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/context-handlers.xml")
@WebServlet(name="BACoordinatorCompletion", urlPatterns={"/BACoordinatorCompletion"})
public class BACoordinatorCompletionService implements BACoordinatorCompletion {
    private static final Logger log = Logger.getLogger(BACoordinatorCompletionService.class);
    private EventLog eventLog = new EventLog();
    
    /**
     * Add an item to a set and enroll a Participant if necessary then pass the call through to the business logic.
     * 
     * @param value the value to add to the set.
     * @throws AlreadyInSetException if value is already in the set
     * @throws SetServiceException if an error occurred when attempting to add the item to the set.
     */
    @WebMethod
    public void saveData(String value, ServiceCommand... serviceCommands) throws TestApplicationException {

        log.info("[BA COORDINATOR COMPL SERVICE] web method saveData('" + value + "')");
        eventLog.foundEventLogName(value);
        BusinessActivityManager activityManager = BusinessActivityManagerFactory.businessActivityManager();
        
        // transaction context associated with this thread
        String transactionId;
        try {
            transactionId = activityManager.currentTransaction().toString();
        } catch (SystemException e) {
            throw new RuntimeException("Unable to lookup existing busines activity", e);
        }

         // Lookup existing participant or register new participant (
        BACoordinationCompletionParticipant participantBA = BACoordinationCompletionParticipant.getSomeParticipant(transactionId);

        if (participantBA != null && ServiceCommand.isPresent(REUSE_BA_PARTICIPANT, serviceCommands)) {
            log.info("[BA COORDINATOR COMPL SERVICE] Re-using the existing participant, already registered for this BA - command set to: " + 
                    REUSE_BA_PARTICIPANT);
        } else {
            try {
                // enlist the Participant for this service:
                participantBA = new BACoordinationCompletionParticipant(serviceCommands, eventLog, transactionId, value);
                BACoordinationCompletionParticipant.recordParticipant(transactionId, participantBA);

                log.info("[BA COORDINATOR COMPL SERVICE] Enlisting a participant into the BA");
                BAParticipantManager baParticipantManager =  activityManager.enlistForBusinessAgreementWithCoordinatorCompletion(participantBA, 
                        "BACoordinatorCompletition:" + new Uid().toString());

                if (ServiceCommand.isPresent(CANNOT_COMPLETE, serviceCommands)) {
                    baParticipantManager.cannotComplete();
                    return;
                }
                
                if (ServiceCommand.isPresent(DO_COMPLETE, serviceCommands)) {
                    throw new RuntimeException("Only ParticipantCompletion participants are supposed to call complete. " +
                    		"CoordinatorCompletion participants need to wait to be notified by the coordinator.");
                }

            } catch (Exception e) {
                log.error("[BA COORDINATOR COMPL SERVICE] Participant enlistment failed");
                e.printStackTrace(System.err);
                throw new RuntimeException("Error enlisting participant", e);
            }
        }
        
        // calling a method on participant
        participantBA.addValue(value);
        
        if (ServiceCommand.isPresent(APPLICATION_EXCEPTION, serviceCommands)) {
            throw new TestApplicationException("Intentionally thrown Application Exception - service command set to: " + APPLICATION_EXCEPTION);
        }        

        // invoke the back-end business logic
        log.info("[BA COORDINATOR COMPL SERVICE] Invoking the back-end business logic - saving value: " + value);
        MockSet.add(value);
    }

    @WebMethod
    public EventLog getEventLog() {
        return eventLog;
    }

    @WebMethod
    public void clearEventLog() {
        eventLog.clear();
    }
}
