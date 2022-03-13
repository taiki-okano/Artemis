package de.tum.in.www1.artemis.service.messaging.services;

import java.util.Set;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;

/**
 * Message producer to communicate with the user management microservice through message queue to send messages related to user creation.
 */
@Component
@EnableJms
public class UserServiceProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceProducer.class);

    @Autowired
    private final JmsTemplate jmsTemplate;

    public UserServiceProducer(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * Send a message to create internal user using the given fields.
     *
     * @param login              the login of the user
     * @param password           the password of the user
     * @param groups             the groups of the user
     * @param firstName          the first name of the user
     * @param lastName           the last name of the user
     * @param email              the email of the user
     * @param registrationNumber the registration number of the user
     * @param imageUrl           the image url of the user
     * @param langKey            the language key of the user
     * @param isInternal         flas if the user is internal user
     * @return the created user
     */
    public User createInternalUser(String login, @Nullable String password, @Nullable Set<String> groups, String firstName, String lastName, String email,
            String registrationNumber, String imageUrl, String langKey, boolean isInternal) {
        LOGGER.info("Create internal user");
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setGroups(groups);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setRegistrationNumber(registrationNumber);
        user.setImageUrl(imageUrl);
        user.setLangKey(langKey);
        user.setInternal(isInternal);

        String correlationId = Integer.toString(user.hashCode());
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER, user, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
    }

    /**
     * Send a message in order to create a user
     *
     * @param userDTO the user settings
     * @return the created user
     */
    public User createUser(ManagedUserVM userDTO) {
        String correlationId = Integer.toString(userDTO.hashCode());
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER, userDTO, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
    }

    /**
     * Send a message in order to activate a user accont
     *
     * @param userDTO the user to activate
     */
    public void activateUser(User userDTO) {
        LOGGER.info("Activate user");
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ACTIVATE_USER, userDTO);
    }

    /**
     * Send a message to request a password reset of a user
     *
     * @param mail the email of the user
     * @return the updated user
     */
    public User requestPasswordReset(String mail) {
        LOGGER.info("Request password reset for user with mail {}", mail);
        String correlationId = Integer.toString(mail.hashCode());
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET, mail, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            return null;
        }
    }

    /**
     * Send a message to request a password reset of a user
     *
     * @param user the user
     * @return the updated user
     */
    public boolean prepareUserForPasswordReset(User user) {
        LOGGER.info("Request password reset for user {}", user);
        String correlationId = Integer.toString(user.hashCode());
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET, user, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, responseMessage);
        try {
            return responseMessage.getBody(Boolean.class);
        }
        catch (JMSException e) {
            return false;
        }
    }

    /**
     * Send a message to update a user
     *
     * @param userDTO the user data
     * @return the updated user
     */
    public User saveUser(User userDTO) {
        String correlationId = Integer.toString(userDTO.hashCode());
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER, userDTO, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
    }
}
