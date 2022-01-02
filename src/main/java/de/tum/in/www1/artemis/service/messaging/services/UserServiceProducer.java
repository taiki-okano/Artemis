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

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;

/**
 * Message producer to communicate with the user management microservice through message queue to send messages related to user creation.
 */
@Component
@EnableJms
public class UserServiceProducer {

    public static final String USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER = "user_management_queue.create_internal_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP = "user_management_queue.create_internal_user_resp";

    public static final String USER_MANAGEMENT_QUEUE_ACTIVATE_USER = "user_management_queue.activate_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_USER = "user_management_queue.create_user";

    public static final String USER_MANAGEMENT_QUEUE_CREATE_USER_RESP = "user_management_queue.create_user_resp";

    public static final String USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET = "user_management_queue.request_password_reset";

    public static final String USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP = "user_management_queue.request_password_reset_resp";

    public static final String USER_MANAGEMENT_QUEUE_SAVE_USER = "user_management_queue.save_user";

    public static final String USER_MANAGEMENT_QUEUE_SAVE_USER_RESP = "user_management_queue.save_user_resp";

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
     * @return the created user
     */
    public User createInternalUser(String login, @Nullable String password, @Nullable Set<String> groups, String firstName, String lastName, String email,
            String registrationNumber, String imageUrl, String langKey) {
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

        String correlationId = Integer.toString(user.hashCode());
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER, user, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, responseMessage);
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
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_CREATE_USER, userDTO, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, responseMessage);
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
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_ACTIVATE_USER, userDTO);
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
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET, mail, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            return null;
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
        jmsTemplate.convertAndSend(USER_MANAGEMENT_QUEUE_SAVE_USER, userDTO, message -> {
            message.setJMSCorrelationID(correlationId);
            return message;
        });
        Message responseMessage = jmsTemplate.receiveSelected(USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, "JMSCorrelationID='" + correlationId + "'");
        LOGGER.info("Received response in queue {} with body {}", USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, responseMessage);
        try {
            return responseMessage.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
    }
}
