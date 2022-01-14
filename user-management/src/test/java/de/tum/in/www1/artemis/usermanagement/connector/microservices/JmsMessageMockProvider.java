package de.tum.in.www1.artemis.usermanagement.connector.microservices;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Component
public class JmsMessageMockProvider {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Mock
    private Message message;

    public JmsMessageMockProvider() {
        this.message = Mockito.mock(Message.class);
    }

    public void mockCheckGroupsAvailability() throws JMSException {
        doReturn(message).when(jmsTemplate).receiveSelected(anyString(), anyString());
        doReturn(true).when(message).getBody(Boolean.class);
    }

}
