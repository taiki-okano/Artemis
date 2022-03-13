package de.tum.in.www1.artemis.usermanagement.service.user;

import de.tum.in.www1.artemis.domain.Authority;
import de.tum.in.www1.artemis.domain.GuidedTourSetting;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.exception.VersionControlException;
import de.tum.in.www1.artemis.repository.AuthorityRepository;
import de.tum.in.www1.artemis.repository.GuidedTourSettingsRepository;
import de.tum.in.www1.artemis.repository.StudentScoreRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.connectors.CIUserManagementService;
import de.tum.in.www1.artemis.service.connectors.VcsUserManagementService;
import de.tum.in.www1.artemis.service.dto.UserDTO;
import de.tum.in.www1.artemis.service.dto.UserGroupDTO;
import de.tum.in.www1.artemis.service.user.PasswordService;
import de.tum.in.www1.artemis.usermanagement.exception.AccountRegistrationBlockedException;
import de.tum.in.www1.artemis.usermanagement.exception.UsernameAlreadyUsedException;
import de.tum.in.www1.artemis.usermanagement.service.messaging.InstanceMessageSendService;
import de.tum.in.www1.artemis.usermanagement.service.messaging.services.UserServiceProducer;
import de.tum.in.www1.artemis.usermanagement.web.rest.errors.EmailAlreadyUsedException;
import de.tum.in.www1.artemis.usermanagement.web.rest.errors.InvalidPasswordException;
import de.tum.in.www1.artemis.web.rest.vm.ManagedUserVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.security.RandomUtil;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tum.in.www1.artemis.domain.Authority.ADMIN_AUTHORITY;
import static de.tum.in.www1.artemis.security.Role.ADMIN;
import static de.tum.in.www1.artemis.security.Role.STUDENT;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

    public static final int ONE_DAY_IN_SECONDS = 86400;

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${artemis.user-management.internal-admin.username:#{null}}")
    private Optional<String> artemisInternalAdminUsername;

    @Value("${artemis.user-management.internal-admin.password:#{null}}")
    private Optional<String> artemisInternalAdminPassword;

    private final UserCreationService userCreationService;

    private final UserRepository userRepository;

    private final PasswordService passwordService;

    private final Optional<VcsUserManagementService> optionalVcsUserManagementService;

    private final Optional<CIUserManagementService> optionalCIUserManagementService;

    private final StudentScoreRepository studentScoreRepository;

    private final CacheManager cacheManager;

    private final AuthorityRepository authorityRepository;

    private final GuidedTourSettingsRepository guidedTourSettingsRepository;

    private final InstanceMessageSendService instanceMessageSendService;

    private final UserServiceProducer userServiceProducer;

    public UserService(UserCreationService userCreationService, UserRepository userRepository, AuthorityRepository authorityRepository,
                       CacheManager cacheManager, GuidedTourSettingsRepository guidedTourSettingsRepository, PasswordService passwordService,
                       Optional<VcsUserManagementService> optionalVcsUserManagementService, Optional<CIUserManagementService> optionalCIUserManagementService,
                       StudentScoreRepository studentScoreRepository, InstanceMessageSendService instanceMessageSendService, UserServiceProducer userServiceProducer) {
        this.userCreationService = userCreationService;
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.cacheManager = cacheManager;
        this.guidedTourSettingsRepository = guidedTourSettingsRepository;
        this.passwordService = passwordService;
        this.optionalVcsUserManagementService = optionalVcsUserManagementService;
        this.optionalCIUserManagementService = optionalCIUserManagementService;
        this.studentScoreRepository = studentScoreRepository;
        this.instanceMessageSendService = instanceMessageSendService;
        this.userServiceProducer = userServiceProducer;
    }

    /**
     * Make sure that the internal artemis admin (in case it is defined in the yml configuration) is available in the database
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {

        try {
            if (artemisInternalAdminUsername.isPresent() && artemisInternalAdminPassword.isPresent()) {
                // authenticate so that db queries are possible
                SecurityUtils.setAuthorizationObject();
                Optional<User> existingInternalAdmin = userRepository.findOneWithGroupsAndAuthoritiesByLogin(artemisInternalAdminUsername.get());
                if (existingInternalAdmin.isPresent()) {
                    log.info("Update internal admin user {}", artemisInternalAdminUsername.get());
                    existingInternalAdmin.get().setPassword(passwordService.encodePassword(artemisInternalAdminPassword.get()));
                    // needs to be mutable --> new HashSet<>(Set.of(...))
                    existingInternalAdmin.get().setAuthorities(new HashSet<>(Set.of(ADMIN_AUTHORITY, new Authority(STUDENT.getAuthority()))));
                    saveUser(existingInternalAdmin.get());
                    updateUserInConnectorsAndAuthProvider(existingInternalAdmin.get(), existingInternalAdmin.get().getLogin(), existingInternalAdmin.get().getGroups());
                }
                else {
                    log.info("Create internal admin user {}", artemisInternalAdminUsername.get());
                    ManagedUserVM userDto = new ManagedUserVM();
                    userDto.setLogin(artemisInternalAdminUsername.get());
                    userDto.setPassword(artemisInternalAdminPassword.get());
                    userDto.setActivated(true);
                    userDto.setFirstName("Administrator");
                    userDto.setLastName("Administrator");
                    userDto.setEmail("admin@localhost");
                    userDto.setLangKey("en");
                    userDto.setCreatedBy("system");
                    userDto.setLastModifiedBy("system");
                    // needs to be mutable --> new HashSet<>(Set.of(...))
                    userDto.setAuthorities(new HashSet<>(Set.of(ADMIN.getAuthority(), STUDENT.getAuthority())));
                    userDto.setGroups(new HashSet<>());
                    userCreationService.createUser(userDto);
                }
            }
        }
        catch (Exception ex) {
            log.error("An error occurred after application startup when creating or updating the admin user or in the LDAP search", ex);
        }
    }

    /**
     * Activate user registration
     *
     * @param key activation key for user registration
     * @return user if user exists otherwise null
     */
    public Optional<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneWithGroupsByActivationKey(key).map(user -> {
            activateUser(user);
            return user;
        });
    }

    /**
     * Activates the user and cancels the automatic cleanup of the account.
     *
     * @param user the non-activated user
     */
    public void activateUser(User user) {
        // Cancel automatic removal of the user since it's activated.
        instanceMessageSendService.sendCancelRemoveNonActivatedUserSchedule(user.getId());
        optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.activateUser(user.getLogin()));
        // activate given user for the registration key.
        userCreationService.activateUser(user);
    }

    /**
     * Reset user password for given reset key
     *
     * @param newPassword new password string
     * @param key         reset key
     * @return user for whom the password was performed
     */
    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository.findOneByResetKey(key).filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(ONE_DAY_IN_SECONDS))).map(user -> {
            user.setPassword(passwordService.encodePassword(newPassword));
            user.setResetKey(null);
            user.setResetDate(null);
            saveUser(user);
            optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(user.getLogin(), user, null, null, true));
            optionalCIUserManagementService.ifPresent(ciUserManagementService -> ciUserManagementService.updateUser(user));
            return user;
        });
    }

    /**
     * saves the user and clears the cache
     *
     * @param user the user object that will be saved into the database
     * @return the saved and potentially updated user object
     */
    public User saveUser(User user) {
        clearUserCaches(user);
        log.debug("Save user {}", user);
        return userRepository.save(user);
    }

    /**
     * Set password reset data for a user if eligible
     *
     * @param user user requesting reset
     * @return true if the user is eligible
     */
    public boolean prepareUserForPasswordReset(User user) {
        if (user.getActivated() && user.isInternal()) {
            user.setResetKey(RandomUtil.generateResetKey());
            user.setResetDate(Instant.now());
            saveUser(user);
            return true;
        }
        return false;
    }

    /**
     * Request password reset for user email
     *
     * @param mail to find user
     * @return user if user exists otherwise null
     */
    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail).filter(User::getActivated).map(user -> {
            user.setResetKey(RandomUtil.generateResetKey());
            user.setResetDate(Instant.now());
            return saveUser(user);
        });
    }

    /**
     * Register user and create it only in the internal Artemis database. This is a pure service method without any logic with respect to external systems.
     *
     * @param userDTO  user data transfer object
     * @param password string
     * @return newly registered user or throw registration exception
     */
    public User registerUser(UserDTO userDTO, String password) {
        // Prepare the new user object.
        final var newUser = new User();
        String encryptedPassword = passwordService.encodePassword(password);
        newUser.setLogin(userDTO.getLogin().toLowerCase());
        // new user gets initially a generated password
        newUser.setPassword(encryptedPassword);
        newUser.setFirstName(userDTO.getFirstName());
        newUser.setLastName(userDTO.getLastName());
        newUser.setEmail(userDTO.getEmail().toLowerCase());
        newUser.setImageUrl(userDTO.getImageUrl());
        newUser.setLangKey(userDTO.getLangKey());
        // new user is not active
        newUser.setActivated(false);
        // new user gets registration key
        newUser.setActivationKey(RandomUtil.generateActivationKey());
        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(STUDENT.getAuthority()).ifPresent(authorities::add);
        newUser.setAuthorities(authorities);

        // Find user that has the same login
        Optional<User> optionalExistingUser = userRepository.findOneWithGroupsByLogin(userDTO.getLogin().toLowerCase());
        if (optionalExistingUser.isPresent()) {
            User existingUser = optionalExistingUser.get();
            return handleRegisterUserWithSameLoginAsExistingUser(newUser, existingUser);
        }

        // Find user that has the same email
        optionalExistingUser = userRepository.findOneWithGroupsByEmailIgnoreCase(userDTO.getEmail());
        if (optionalExistingUser.isPresent()) {
            User existingUser = optionalExistingUser.get();

            // An account with the same login is already activated.
            if (existingUser.getActivated()) {
                throw new EmailAlreadyUsedException();
            }

            // The email is different which means that the user wants to re-register the same
            // account with a different email. Block this.
            throw new AccountRegistrationBlockedException(newUser.getEmail());
        }

        // we need to save first so that the user can be found in the database in the subsequent method
        User savedNonActivatedUser = saveUser(newUser);

        // Create an account on the VCS. If it fails, abort registration.
        optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> {
            try {
                vcsUserManagementService.createVcsUser(savedNonActivatedUser);
                vcsUserManagementService.deactivateUser(savedNonActivatedUser.getLogin());
            }
            catch (VersionControlException e) {
                log.error("An error occurred while registering GitLab user " + savedNonActivatedUser.getLogin() + ":", e);
                deleteUser(savedNonActivatedUser);
                throw e;
            }
        });

        // Automatically remove the user if it wasn't activated after a certain amount of time.
        instanceMessageSendService.sendRemoveNonActivatedUserSchedule(savedNonActivatedUser.getId());

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    /**
     * Handles the case where a user registers a new account but a user with the same login already
     * exists in Artemis.
     *
     * @param newUser the new user
     * @param existingUser the existing user
     * @return the existing non-activated user in Artemis.
     */
    private User handleRegisterUserWithSameLoginAsExistingUser(User newUser, User existingUser) {
        // An account with the same login is already activated.
        if (existingUser.getActivated()) {
            throw new UsernameAlreadyUsedException();
        }

        // The user has the same login and email, but the account is not activated.
        // Return the existing non-activated user so that Artemis can re-send the
        // activation link.
        if (existingUser.getEmail().equals(newUser.getEmail())) {
            // Update the existing user and VCS
            newUser.setId(existingUser.getId());
            User updatedExistingUser = userRepository.save(newUser);
            optionalVcsUserManagementService
                    .ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(existingUser.getLogin(), updatedExistingUser, Set.of(), Set.of(), true));

            // Post-pone the cleaning up of the account
            instanceMessageSendService.sendRemoveNonActivatedUserSchedule(updatedExistingUser.getId());
            return updatedExistingUser;
        }

        // The email is different which means that the user wants to re-register the same
        // account with a different email. Block this.
        throw new AccountRegistrationBlockedException(existingUser.getEmail());
    }

    /**
     * Updates the user (and synchronizes its password) and its groups in the connected version control system (e.g. GitLab if available).
     * Also updates the user groups in the used authentication provider (like JiraAuthenticationProvider)}.
     *
     * @param oldUserLogin The username of the user. If the username is updated in the user object, it must be the one before the update in order to find the user in the VCS
     * @param user         The updated user in Artemis (this method assumes that the user including its groups was already saved to the Artemis database)
     * @param oldGroups    The old groups of the user before the update
     */
    public void updateUserInConnectorsAndAuthProvider(User user, String oldUserLogin, Set<String> oldGroups) {
        final var updatedGroups = user.getGroups();
        final var removedGroups = oldGroups.stream().filter(group -> !updatedGroups.contains(group)).collect(Collectors.toSet());
        final var addedGroups = updatedGroups.stream().filter(group -> !oldGroups.contains(group)).collect(Collectors.toSet());
        optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(oldUserLogin, user, removedGroups, addedGroups, true));
        optionalCIUserManagementService.ifPresent(ciUserManagementService -> ciUserManagementService.updateUserAndGroups(oldUserLogin, user, addedGroups, removedGroups));

        userServiceProducer.updateUserGroups(new UserGroupDTO(user, removedGroups, addedGroups));
    }

    /**
     * Delete user based on login string
     *
     * @param login user login string
     */
    @Transactional // ok because entities are deleted
    public void deleteUser(String login) {
        // Delete the user in the connected VCS if necessary (e.g. for GitLab)
        optionalVcsUserManagementService.ifPresent(userManagementService -> userManagementService.deleteVcsUser(login));
        // Delete the user in the local Artemis database
        userRepository.findOneByLogin(login).ifPresent(user -> {
            optionalCIUserManagementService.ifPresent(ciUserManagementService -> ciUserManagementService.deleteUser(user));
            deleteUser(user);
            log.warn("Deleted User: {}", user);
        });
    }

    @Transactional // ok because entities are deleted
    protected void deleteUser(User user) {
        // TODO: before we can delete the user, we need to make sure that all associated objects are deleted as well (or the connection to user is set to null)
        // 1) All participation connected to the user (as student)
        // 2) All notifications connected to the user
        // 3) All results connected to the user (as assessor)
        // 4) All complaints and complaints responses associated to the user
        // 5) All student exams associated to the user
        // 6) All LTIid and LTIOutcomeUrls associated to the user
        // 7) All Post and AnswerPost
        // 8) Remove the user from its teams
        // 9) Delete the submissionVersion / remove the user from the submissionVersion
        // 10) Delete the tutor participation

        studentScoreRepository.deleteAllByUser(user);

        userRepository.delete(user);
        clearUserCaches(user);
        userRepository.flush();
    }

    /**
     * Change password of current user
     *
     * @param currentClearTextPassword cleartext password
     * @param newPassword              new password string
     */
    public void changePassword(String currentClearTextPassword, String newPassword) {
        SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(user -> {
            String currentEncryptedPassword = user.getPassword();
            if (!passwordService.checkPasswordMatch(currentClearTextPassword, currentEncryptedPassword)) {
                throw new InvalidPasswordException();
            }
            String encryptedPassword = passwordService.encodePassword(newPassword);
            user.setPassword(encryptedPassword);
            saveUser(user);
            optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(user.getLogin(), user, null, null, true));
            optionalCIUserManagementService.ifPresent(ciUserManagementService -> ciUserManagementService.updateUser(user));

            log.debug("Changed password for User: {}", user);
        });
    }

    private void clearUserCaches(User user) {
        var userCache = cacheManager.getCache(User.class.getName());
        if (userCache != null) {
            userCache.evict(user.getLogin());
        }
    }

    /**
     * Update the guided tour settings of the currently logged in user
     *
     * @param guidedTourSettings the updated set of guided tour settings
     * @return the updated user object with the changed guided tour settings
     */
    public User updateGuidedTourSettings(Set<GuidedTourSetting> guidedTourSettings) {
        User loggedInUser = userRepository.getUserWithGroupsAuthoritiesAndGuidedTourSettings();
        loggedInUser.getGuidedTourSettings().clear();
        for (GuidedTourSetting setting : guidedTourSettings) {
            loggedInUser.addGuidedTourSetting(setting);
            guidedTourSettingsRepository.save(setting);
        }
        // TODO: do we really need to save the user here, or is it enough if we save in the guidedTourSettingsRepository?
        return saveUser(loggedInUser);
    }

    /**
     * Delete a given guided tour setting of the currently logged in user (e.g. when the user restarts a guided tutorial)
     *
     * @param guidedTourSettingsKey the key of the guided tour setting that should be deleted
     * @return the updated user object without the deleted guided tour setting
     */
    public User deleteGuidedTourSetting(String guidedTourSettingsKey) {
        User loggedInUser = userRepository.getUserWithGroupsAuthoritiesAndGuidedTourSettings();
        Set<GuidedTourSetting> guidedTourSettings = loggedInUser.getGuidedTourSettings();
        for (GuidedTourSetting setting : guidedTourSettings) {
            if (setting.getGuidedTourKey().equals(guidedTourSettingsKey)) {
                loggedInUser.removeGuidedTourSetting(setting);
                break;
            }
        }
        return saveUser(loggedInUser);
    }

    @Transactional
    public void updateUserNotificationVisibility(Long userId, ZonedDateTime hideUntil) {
        userRepository.updateUserNotificationVisibility(userId, hideUntil);
    }
}
