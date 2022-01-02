package de.tum.in.www1.artemis.security;

import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.service.messaging.services.UserServiceProducer;
import de.tum.in.www1.artemis.service.user.PasswordService;

public abstract class ArtemisAuthenticationProviderImpl implements ArtemisAuthenticationProvider {

    protected final UserServiceProducer userServiceProducer;

    protected final UserRepository userRepository;

    protected final PasswordService passwordService;

    public ArtemisAuthenticationProviderImpl(UserRepository userRepository, PasswordService passwordService, UserServiceProducer userServiceProducer) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.userServiceProducer = userServiceProducer;
    }
}
