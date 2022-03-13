package de.tum.in.www1.artemis.connector.microservices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.HashSet;

import javax.jms.JMSException;
import javax.jms.Message;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.config.MessageBrokerConstants;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.user.PasswordService;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;
import tech.jhipster.security.RandomUtil;

@SpringBootTest
@Component
public class UserManagementJmsMessageMockProvider {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private Message message;

    private final PasswordService passwordService;

    public UserManagementJmsMessageMockProvider(PasswordService passwordService) {
        this.passwordService = passwordService;
        this.message = Mockito.mock(Message.class);
    }

    public void mockSendAndReceiveCreateUser() {
        doAnswer(invocation -> {
            ManagedUserVM managedUserVM = invocation.getArgument(1);
            User user = new User();
            user.setLogin(managedUserVM.getLogin());
            user.setFirstName(managedUserVM.getFirstName());
            user.setLastName(managedUserVM.getLastName());
            user.setEmail(managedUserVM.getEmail());
            user.setImageUrl(managedUserVM.getImageUrl());
            if (managedUserVM.getLangKey() == null) {
                user.setLangKey(Constants.DEFAULT_LANGUAGE);
            }
            else {
                user.setLangKey(managedUserVM.getLangKey());
            }

            String encryptedPassword = passwordService.encodePassword(managedUserVM.getPassword() == null ? RandomUtil.generatePassword() : managedUserVM.getPassword());
            user.setPassword(encryptedPassword);
            user.setResetKey(RandomUtil.generateResetKey());
            user.setResetDate(Instant.now());
            user.setActivated(true);
            user.setInternal(true);
            user.setRegistrationNumber(managedUserVM.getVisibleRegistrationNumber());
            userRepository.save(user);
            doReturn(user).when(message).getBody(User.class);

            return null;
        }).when(jmsTemplate).convertAndSend(eq(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_USER), any(), any());

        doReturn(message).when(jmsTemplate).receiveSelected(anyString(), anyString());
    }

    public void mockSendAndReceiveSaveUser() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(1);
            userRepository.save(user);
            doReturn(user).when(message).getBody(User.class);

            return null;
        }).when(jmsTemplate).convertAndSend(eq(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_SAVE_USER), any(), any());

        doReturn(message).when(jmsTemplate).receiveSelected(anyString(), anyString());
    }

    public void mockSendAndReceiveCreateInternalUser() {
        doAnswer(invocation -> {
            User user = invocation.getArgument(1);
            String encryptedPassword = passwordService.encodePassword(user.getPassword());
            user.setPassword(encryptedPassword);
            user.setActivated(false);
            user.setActivationKey(RandomUtil.generateActivationKey());
            user.setGroups(user.getGroups() != null ? new HashSet<>(user.getGroups()) : new HashSet<>());

            userRepository.save(user);
            doReturn(user).when(message).getBody(User.class);

            return null;
        }).when(jmsTemplate).convertAndSend(eq(MessageBrokerConstants.USER_MANAGEMENT_QUEUE_CREATE_INTERNAL_USER), any(), any());

        doReturn(message).when(jmsTemplate).receiveSelected(anyString(), anyString());
    }

    public void mockPrepareUserForPasswordReset() throws JMSException {
        doReturn(true).when(message).getBody(Boolean.class);
        doReturn(message).when(jmsTemplate).receiveSelected(anyString(), anyString());
    }
}
