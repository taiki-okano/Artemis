package de.tum.in.www1.artemis.service.messaging.services;

import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import de.tum.in.www1.artemis.security.ArtemisAuthenticationProvider;
import de.tum.in.www1.artemis.service.dto.UserGroupDTO;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;

/**
 * Message producer to communicate with the user management microservice through message queue
 */
@Component
@EnableJms
public class UserServiceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceConsumer.class);

    private ArtemisAuthenticationProvider artemisAuthenticationProvider;

    @Autowired
    private final JmsTemplate jmsTemplate;

    public UserServiceConsumer(JmsTemplate jmsTemplate, ArtemisAuthenticationProvider artemisAuthenticationProvider) {
        this.jmsTemplate = jmsTemplate;
        this.artemisAuthenticationProvider = artemisAuthenticationProvider;
    }

    /**
     * Consumes and responds to messages related to checking whether groups are available.
     *
     * @param message the message coming from the message broker
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE)
    public void areGroupsAvailable(Message message) {
        LOGGER.info("Receive message in queue {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE);
        Set<String> groups;
        try {
            groups = message.getBody(Set.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        boolean groupsAvailable = groups.stream().allMatch(group -> artemisAuthenticationProvider.isGroupAvailable(group));
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, groupsAvailable);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, groupsAvailable, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Consumes and responds to messages related to checking whether groups are available.
     *
     * @param message the message coming from the message broker
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_UPDATE_USER_GROUPS)
    public void updateUserGroups(Message message) {
        LOGGER.info("Receive message in queue {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_UPDATE_USER_GROUPS);
        UserGroupDTO userGroupDTO;
        try {
            userGroupDTO = message.getBody(UserGroupDTO.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        if (!CollectionUtils.isEmpty(userGroupDTO.getAddedGroups())) {
            userGroupDTO.getAddedGroups().forEach(group -> artemisAuthenticationProvider.addUserToGroup(userGroupDTO.getUser(), group));
        }
        if (!CollectionUtils.isEmpty(userGroupDTO.getRemovedGroups())) {
            userGroupDTO.getRemovedGroups().forEach(group -> artemisAuthenticationProvider.removeUserFromGroup(userGroupDTO.getUser(), group));
        }
    }
}
