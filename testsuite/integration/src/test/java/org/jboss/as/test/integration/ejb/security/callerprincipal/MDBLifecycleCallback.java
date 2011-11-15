package org.jboss.as.test.integration.ejb.security.callerprincipal;


import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.MessageDriven;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

import javax.ejb.ActivationConfigProperty;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/test")
})
@SecurityDomain("ejb3-tests")
@ResourceAdapter(value = "hornetq-ra.rar") // based on documentation to as 7
public class MDBLifecycleCallback implements MessageDrivenBean, MessageListener {
    private static final long serialVersionUID = 1L;

    private Logger log = Logger.getLogger(MDBLifecycleCallback.class); 
    
    @Resource(mappedName="java:/ConnectionFactory")
    private QueueConnectionFactory qFactory;
    
    @Resource
    private MessageDrivenContext msgContext;
    
    private MessageDrivenContext ctx;
    
    //TODO: test @PreDestroy    
    @PostConstruct
    public void init() throws Exception {
        // on MDB is not permitted to call getCallerPrincipal on @PostConstruct
        log.info("message context: " + msgContext + ", message context2: " + ctx);

        // TestResultsSingleton sti = (TestResultsSingleton) newctx.lookup("java:module/" + TestResultsSingleton.class.getSimpleName());
        // Principal princ = msgContext.getCallerPrincipal();
        // sti.addMdb(princ);
        log.info("postConstruct " + MDBLifecycleCallback.class.getSimpleName());
    }
    
    public void setMessageDrivenContext( MessageDrivenContext ctx ) throws EJBException {
        this.ctx = ctx;
    }
    
    public void ejbRemove() throws EJBException {
        //
    }
    
    public void onMessage(Message message)
    {
       log.info("onMessage received msg: " + message.toString());
       try
       {
          try
          {
             sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID(), null);
          }
          catch(Exception e)
          {
             sendReply((Queue) message.getJMSReplyTo(), message.getJMSMessageID(), e);
          }
       }
       catch(JMSException e)
       {
          throw new RuntimeException(e);
       }
    }
    
    private void sendReply(Queue destination, String messageID, Exception e) throws JMSException
    {
       QueueConnection conn = qFactory.createQueueConnection();
       try
       {
          QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
          QueueSender sender = session.createSender(destination);
          ObjectMessage message = session.createObjectMessage(e == null ? "SUCCESS" : e);
          message.setJMSCorrelationID(messageID);
          sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
       }
       finally
       {
          conn.close();
       }
    }
}