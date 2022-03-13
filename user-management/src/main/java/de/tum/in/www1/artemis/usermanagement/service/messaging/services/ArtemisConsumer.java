package de.tum.in.www1.artemis.usermanagement.service.messaging.services;

import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.usermanagement.service.user.UserCreationService;
import de.tum.in.www1.artemis.usermanagement.service.user.UserService;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Optional;

/**
 * JMS Consumer to consume messages from Artemis microservice related to user management.
 */
@Component
@EnableJms
public class ArtemisConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArtemisConsumer.class);

    @Autowired
    private final JmsTemplate jmsTemplate;

    private UserCreationService userCreationService;

    private UserService userService;

    public ArtemisConsumer(UserCreationService userCreationService, UserService userService, JmsTemplate jmsTemplate) {
        this.userCreationService = userCreationService;
        this.userService = userService;
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Request password reset for a user
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET)
    public void requestPasswordReset(Message message) {
        String userEmail;
        try {
            userEmail = message.getBody(String.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        Optional<User> updatedUser = userService.requestPasswordReset(userEmail);
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, updatedUser);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, updatedUser.isPresent() ? updatedUser.get() : false, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Request password reset for a user
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET)
    public void prepareUserForPasswordReset(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        boolean updatedUser = userService.prepareUserForPasswordReset(user);
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, updatedUser);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, updatedUser, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Create a user.
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER)
    public void createUser(Message message) {
        ManagedUserVM managedUserVM;
        try {
            managedUserVM = message.getBody(ManagedUserVM.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        User createdUser = userCreationService.createUser(managedUserVM);
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, createdUser);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, createdUser, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Create an internal user.
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER)
    public void createInternalUser(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        User createdUser = userCreationService.createUser(user.getLogin(), user.getPassword(), user.getGroups(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRegistrationNumber(), user.getImageUrl(), user.getLangKey(), user.isInternal());
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, createdUser);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, createdUser, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Update a user.
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER)
    public void saveUser(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        User updatedUser = userService.saveUser(user);
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, updatedUser);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, updatedUser, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    /**
     * Activate user account.
     *
     * @param message the message to consume
     */
    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ACTIVATE_USER)
    public void activateUser(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        } catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        userService.activateUser(user);
    }

}
