package de.tum.in.www1.artemis.usermanagement.service.messaging.services;

import de.tum.in.www1.artemis.service.dto.UserGroupDTO;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Set;

/**
 * Message producer which sends messages to the message queue related to user groups.
 */
@Component
@EnableJms
public class UserServiceProducer {
    public static final String USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE = "user_management_queue.are_groups_available";
    public static final String USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP = "user_management_queue.are_groups_available_resp";
    public static final String USER_MANAGEMENT_QUEUE_UPDATE_USER_GROUPS = "user_management_queue.update_user_groups";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceProducer.class);

    @Autowired
    private final JmsTemplate jmsTemplate;

    public UserServiceProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Check if all groups are available by sending a message in a queue and waiting for a response.
     *
     * @param groups the set of groups to check
     * @return true if all groups are available, false otherwise
     */
    public boolean areGroupsAvailable(Set<String> groups) {
        LOGGER.info("Check if groups {} are available", groups);
        String correlationId = Integer.toString(groups.hashCode());
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE, groups, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, "JMSCorrelationID='"+correlationId+"'");
        LOGGER.info("Received response in queue {} with body {}", USER_MANAGEMENT_QUEUE_ARE_GROUPS_AVAILABLE_RESP, responseMessage);
        try {
            return responseMessage.getBody(Boolean.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
    }

    /**
     * Send a message to request to update groups of a user.
     *
     * @param userGroupDTO the user data and the groups to remove and add
     */
    public void updateUserGroups(UserGroupDTO userGroupDTO) {
        LOGGER.info("Update the groups {}", userGroupDTO);
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_UPDATE_USER_GROUPS, userGroupDTO);
    }
}
