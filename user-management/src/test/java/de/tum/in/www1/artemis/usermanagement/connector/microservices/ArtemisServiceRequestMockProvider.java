package de.tum.in.www1.artemis.usermanagement.connector.microservices;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.user.PasswordService;
import de.tum.in.www1.artemis.usermanagement.service.messaging.services.UserServiceProducer;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tech.jhipster.security.RandomUtil;

import javax.jms.JMSException;
import javax.jms.Message;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
@EnableJms
public class ArtemisServiceRequestMockProvider {
    @Autowired
    private final JmsTemplate jmsTemplate;

    public ArtemisServiceRequestMockProvider(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = UserServiceProducer.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE)
    public void checkGroupsAvailability(Message message) {
        jmsTemplate.convertAndSend(UserServiceProducer.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, true, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }
}
