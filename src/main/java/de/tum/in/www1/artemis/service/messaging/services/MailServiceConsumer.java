package de.tum.in.www1.artemis.service.messaging.services;

import javax.jms.JMSException;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.service.MailService;
import de.tum.in.www1.artemis.web.rest.errors.InternalServerErrorException;

/**
 * Consumer which consumes all messages that are sent from other services related to sending emails to the user.
 */
@Component
@EnableJms
public class MailServiceConsumer {

    public static final String USER_MANAGEMENT_QUEUE_SEND_ACTIVATION_MAIL = "user_management_queue.send_activation_mail";

    public static final String USER_MANAGEMENT_QUEUE_SEND_PASSWORD_RESET_MAIL = "user_management_queue.send_password_reset_mail";

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceConsumer.class);

    private MailService mailService;

    public MailServiceConsumer(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * Consumes messages related to sending account activation emails and sends the actual email.
     *
     * @param message the message coming from the message broker
     */
    @JmsListener(destination = USER_MANAGEMENT_QUEUE_SEND_ACTIVATION_MAIL)
    public void sendActivationEmail(Message message) {
        LOGGER.info("Receive message in queue {}", USER_MANAGEMENT_QUEUE_SEND_ACTIVATION_MAIL);
        User user;
        try {
            user = message.getBody(User.class);
        }
        catch (JMSException exception) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        mailService.sendActivationEmail(user);
    }

    /**
     * Consumes messages related to sending password reset emails and sends the actual email.
     *
     * @param message the message coming from the message broker
     */
    @JmsListener(destination = USER_MANAGEMENT_QUEUE_SEND_PASSWORD_RESET_MAIL)
    public void sendPasswordResetEmail(Message message) {
        LOGGER.info("Receive message in queue {}", USER_MANAGEMENT_QUEUE_SEND_PASSWORD_RESET_MAIL);
        User user;
        try {
            user = message.getBody(User.class);
        }
        catch (JMSException exception) {
            throw new InternalServerErrorException("There was a problem with the communication between server components. Please try again later!");
        }
        mailService.sendPasswordResetMail(user);
    }
}
