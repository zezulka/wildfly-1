package org.jboss.as.test.integration.messaging.jms.context.cdi;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Queue;

import org.jboss.as.test.integration.messaging.jms.context.auxiliary.TransactedMDB;
import org.jboss.logging.Logger;

// @Transactional
public class MessageSender {
    private static final Logger LOG = Logger.getLogger(MessageSender.class);

    private JMSContext jmsContext;

    @Resource(mappedName = TransactedMDB.QUEUE_NAME)
    private Queue queue;

    @Resource(mappedName = "java:/jms/queue/DLQ")
    private Queue dlq;

    @Inject
    public MessageSender(JMSContext jmsContext) {
        this.jmsContext = jmsContext;
    }

    public MessageSender() {
        // TODO Auto-generated constructor stub
    }

    public void sendMessage(String text) throws JMSException{
        LOG.infof("Sending message '%s' to %s", text, queue.getQueueName());
        jmsContext.createProducer().setJMSReplyTo(dlq).send(queue, text);
    }
}