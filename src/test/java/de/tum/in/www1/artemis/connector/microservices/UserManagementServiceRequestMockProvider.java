package de.tum.in.www1.artemis.connector.microservices;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.user.PasswordService;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;
import tech.jhipster.security.RandomUtil;

@Component
@EnableJms
public class UserManagementServiceRequestMockProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementServiceRequestMockProvider.class);

    @Autowired
    private final JmsTemplate jmsTemplate;

    @Autowired
    private UserRepository userRepository;

    private final PasswordService passwordService;

    public UserManagementServiceRequestMockProvider(JmsTemplate jmsTemplate, PasswordService passwordService) {
        this.jmsTemplate = jmsTemplate;
        this.passwordService = passwordService;
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET)
    public void requestPasswordReset(Message message) {
        LOGGER.info("received message {}", message.toString());

        String userEmail;
        try {
            userEmail = message.getBody(String.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        Optional<User> updatedUser = userRepository.findOneByEmailIgnoreCase(userEmail).filter(User::getActivated).map(user -> {
            user.setResetKey(RandomUtil.generateResetKey());
            user.setResetDate(Instant.now());
            return userRepository.save(user);
        });
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_REQUEST_PASSWORD_RESET_RESP, updatedUser.isPresent() ? updatedUser.get() : false, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER)
    public void createUser(Message message) {
        LOGGER.info("received message {}", message.toString());
        ManagedUserVM managedUserVM;
        try {
            managedUserVM = message.getBody(ManagedUserVM.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }

        User user = new User();
        user.setLogin(managedUserVM.getLogin());
        user.setFirstName(managedUserVM.getFirstName());
        user.setLastName(managedUserVM.getLastName());
        user.setEmail(managedUserVM.getEmail());
        user.setImageUrl(managedUserVM.getImageUrl());
        if (managedUserVM.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        }
        else {
            user.setLangKey(managedUserVM.getLangKey());
        }

        String encryptedPassword = passwordService.encodePassword(managedUserVM.getPassword() == null ? RandomUtil.generatePassword() : managedUserVM.getPassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        user.setRegistrationNumber(managedUserVM.getVisibleRegistrationNumber());
        userRepository.save(user);

        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER_RESP, user, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER)
    public void saveUser(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        userRepository.save(user);
        LOGGER.info("Send response in queue {} with body {}", MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, user);
        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER_RESP, user, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER)
    public void createInternalUser(Message message) {
        User user;
        try {
            user = message.getBody(User.class);
        }
        catch (JMSException e) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }

        String encryptedPassword = passwordService.encodePassword(user.getPassword());
        user.setPassword(encryptedPassword);
        user.setActivated(false);
        user.setActivationKey(RandomUtil.generateActivationKey());
        user.setGroups(user.getGroups() != null ? new HashSet<>(user.getGroups()) : new HashSet<>());

        userRepository.save(user);

        jmsTemplate.convertAndSend(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER_RESP, user, msg -> {
            msg.setJMSCorrelationID(message.getJMSCorrelationID());
            return msg;
        });
    }

    @JmsListener(destination = MessageBrokerConstants.USER_MANAGEMENT_QUEUE_ACTIVATE_USER)
    public void sendActivateUser() {
        // Do nothing
    }

}
