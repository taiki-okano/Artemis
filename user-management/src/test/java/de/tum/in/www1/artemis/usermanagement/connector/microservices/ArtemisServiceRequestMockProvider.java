package de.tum.in.www1.artemis.usermanagement.connector.microservices;

import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.Message;

@Component
@EnableJms
public class ArtemisServiceRequestMockProvider {
    @Autowired
    private final JmsTemplate jmsTemplate;

    public ArtemisServiceRequestMockProvider(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE)
    public void checkGroupsAvailability(Message message) {
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, true, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }
}
