package de.tum.in.www1.artemis.service.user;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.exception.*;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.ArtemisAuthenticationProvider;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.service.connectors.CIUserManagementService;
import de.tum.in.www1.artemis.service.connectors.VcsUserManagementService;
import de.tum.in.www1.artemis.service.ldap.LdapUserDto;
import de.tum.in.www1.artemis.service.ldap.LdapUserService;
import de.tum.in.www1.artemis.service.messaging.services.UserServiceProducer;

/**
 * Service class for managing user groups.
 */
@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${artemis.user-management.use-external}")
    private Boolean useExternalUserManagement;

    private final UserServiceProducer userServiceProducer;

    private final UserRepository userRepository;

    private final AuthorityService authorityService;

    private final Optional<LdapUserService> ldapUserService;

    private final Optional<VcsUserManagementService> optionalVcsUserManagementService;

    private final Optional<CIUserManagementService> optionalCIUserManagementService;

    private final ArtemisAuthenticationProvider artemisAuthenticationProvider;

    private final StudentScoreRepository studentScoreRepository;

    private final CacheManager cacheManager;

    public UserService(UserServiceProducer userServiceProducer, UserRepository userRepository, AuthorityService authorityService, CacheManager cacheManager,
            Optional<LdapUserService> ldapUserService, Optional<VcsUserManagementService> optionalVcsUserManagementService,
            Optional<CIUserManagementService> optionalCIUserManagementService, ArtemisAuthenticationProvider artemisAuthenticationProvider,
            StudentScoreRepository studentScoreRepository) {
        this.userServiceProducer = userServiceProducer;
        this.userRepository = userRepository;
        this.authorityService = authorityService;
        this.cacheManager = cacheManager;
        this.ldapUserService = ldapUserService;
        this.optionalVcsUserManagementService = optionalVcsUserManagementService;
        this.optionalCIUserManagementService = optionalCIUserManagementService;
        this.artemisAuthenticationProvider = artemisAuthenticationProvider;
        this.studentScoreRepository = studentScoreRepository;
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

    // /**
    // <<<<<<< HEAD
    // =======
    // * Set password reset data for a user if eligible
    // *
    // * @param user user requesting reset
    // * @return true if the user is eligible
    // */
    // public boolean prepareUserForPasswordReset(User user) {
    // if (user.getActivated() && user.isInternal()) {
    // user.setResetKey(RandomUtil.generateResetKey());
    // user.setResetDate(Instant.now());
    // saveUser(user);
    // return true;
    // }
    // return false;
    // }

    // /**
    // * Register user and create it only in the internal Artemis database. This is a pure service method without any logic with respect to external systems.
    // *
    // * @param userDTO user data transfer object
    // * @param password string
    // * @return newly registered user or throw registration exception
    // */
    // public User registerUser(UserDTO userDTO, String password) {
    // // Prepare the new user object.
    // final var newUser = new User();
    // String encryptedPassword = passwordService.encodePassword(password);
    // newUser.setLogin(userDTO.getLogin().toLowerCase());
    // // new user gets initially a generated password
    // newUser.setPassword(encryptedPassword);
    // newUser.setFirstName(userDTO.getFirstName());
    // newUser.setLastName(userDTO.getLastName());
    // newUser.setEmail(userDTO.getEmail().toLowerCase());
    // newUser.setImageUrl(userDTO.getImageUrl());
    // newUser.setLangKey(userDTO.getLangKey());
    // // new user is not active
    // newUser.setActivated(false);
    // // new user gets registration key
    // newUser.setActivationKey(RandomUtil.generateActivationKey());
    // Set<Authority> authorities = new HashSet<>();
    // authorityRepository.findById(STUDENT.getAuthority()).ifPresent(authorities::add);
    // newUser.setAuthorities(authorities);
    //
    // // Find user that has the same login
    // Optional<User> optionalExistingUser = userRepository.findOneWithGroupsByLogin(userDTO.getLogin().toLowerCase());
    // if (optionalExistingUser.isPresent()) {
    // User existingUser = optionalExistingUser.get();
    // return handleRegisterUserWithSameLoginAsExistingUser(newUser, existingUser);
    // }
    //
    // // Find user that has the same email
    // optionalExistingUser = userRepository.findOneWithGroupsByEmailIgnoreCase(userDTO.getEmail());
    // if (optionalExistingUser.isPresent()) {
    // User existingUser = optionalExistingUser.get();
    //
    // // An account with the same login is already activated.
    // if (existingUser.getActivated()) {
    // throw new EmailAlreadyUsedException();
    // }
    //
    // // The email is different which means that the user wants to re-register the same
    // // account with a different email. Block this.
    // throw new AccountRegistrationBlockedException(newUser.getEmail());
    // }
    //
    // // we need to save first so that the user can be found in the database in the subsequent method
    // User savedNonActivatedUser = saveUser(newUser);
    //
    // // Create an account on the VCS. If it fails, abort registration.
    // optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> {
    // try {
    // vcsUserManagementService.createVcsUser(savedNonActivatedUser);
    // vcsUserManagementService.deactivateUser(savedNonActivatedUser.getLogin());
    // }
    // catch (VersionControlException e) {
    // log.error("An error occurred while registering GitLab user " + savedNonActivatedUser.getLogin() + ":", e);
    // deleteUser(savedNonActivatedUser);
    // throw e;
    // }
    // });
    //
    // // Automatically remove the user if it wasn't activated after a certain amount of time.
    // instanceMessageSendService.sendRemoveNonActivatedUserSchedule(savedNonActivatedUser.getId());
    //
    // log.debug("Created Information for User: {}", newUser);
    // return newUser;
    // }

    // /**
    // * Handles the case where a user registers a new account but a user with the same login already
    // * exists in Artemis.
    // *
    // * @param newUser the new user
    // * @param existingUser the existing user
    // * @return the existing non-activated user in Artemis.
    // */
    // private User handleRegisterUserWithSameLoginAsExistingUser(User newUser, User existingUser) {
    // // An account with the same login is already activated.
    // if (existingUser.getActivated()) {
    // throw new UsernameAlreadyUsedException();
    // }
    //
    // // The user has the same login and email, but the account is not activated.
    // // Return the existing non-activated user so that Artemis can re-send the
    // // activation link.
    // if (existingUser.getEmail().equals(newUser.getEmail())) {
    // // Update the existing user and VCS
    // newUser.setId(existingUser.getId());
    // User updatedExistingUser = userRepository.save(newUser);
    // optionalVcsUserManagementService
    // .ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(existingUser.getLogin(), updatedExistingUser, Set.of(), Set.of(), true));
    //
    // // Post-pone the cleaning up of the account
    // instanceMessageSendService.sendRemoveNonActivatedUserSchedule(updatedExistingUser.getId());
    // return updatedExistingUser;
    // }
    //
    // // The email is different which means that the user wants to re-register the same
    // // account with a different email. Block this.
    // throw new AccountRegistrationBlockedException(existingUser.getEmail());
    // }

    /**
     * Searches the (optional) LDAP service for a user with the give registration number (= Matrikelnummer) and returns a new Artemis user-
     * Also creates the user in the external user management (e.g. JIRA), in case this is activated
     * Note: this method should only be used if the user does not yet exist in the database
     *
     * @param registrationNumber the matriculation number of the student
     * @return a new user or null if the LDAP user was not found
     */
    public Optional<User> createUserFromLdap(String registrationNumber) {
        if (!StringUtils.hasText(registrationNumber)) {
            return Optional.empty();
        }
        if (ldapUserService.isPresent()) {
            Optional<LdapUserDto> ldapUserOptional = ldapUserService.get().findByRegistrationNumber(registrationNumber);
            if (ldapUserOptional.isPresent()) {
                LdapUserDto ldapUser = ldapUserOptional.get();
                log.info("Ldap User {} has registration number: {}", ldapUser.getUsername(), ldapUser.getRegistrationNumber());

                // handle edge case, the user already exists in Artemis, but for some reason does not have a registration number or it is wrong
                if (StringUtils.hasText(ldapUser.getUsername())) {
                    var existingUser = userRepository.findOneByLogin(ldapUser.getUsername());
                    if (existingUser.isPresent()) {
                        existingUser.get().setRegistrationNumber(ldapUser.getRegistrationNumber());
                        saveUser(existingUser.get());
                        return existingUser;
                    }
                }

                // Use empty password, so that we don't store the credentials of Jira users in the Artemis DB
                // <<<<<<< HEAD
                // User user = userServiceProducer.createInternalUser(ldapUser.getUsername(), "", null, ldapUser.getFirstName(), ldapUser.getLastName(), ldapUser.getEmail(),
                // registrationNumber, null, "en");
                // =======
                User user = userServiceProducer.createInternalUser(ldapUser.getUsername(), "", null, ldapUser.getFirstName(), ldapUser.getLastName(), ldapUser.getEmail(),
                        registrationNumber, null, "en", false);
                // >>>>>>> develop-microservices
                if (useExternalUserManagement) {
                    artemisAuthenticationProvider.createUserInExternalUserManagement(user);
                }
                return Optional.of(user);
            }
            else {
                log.warn("Ldap User with registration number {} not found", registrationNumber);
            }
        }
        return Optional.empty();
    }

    private void clearUserCaches(User user) {
        var userCache = cacheManager.getCache(User.class.getName());
        if (userCache != null) {
            userCache.evict(user.getLogin());
        }
    }

    /**
     * delete the group with the given name
     *
     * @param groupName the name of the group which should be deleted
     */
    public void deleteGroup(String groupName) {
        artemisAuthenticationProvider.deleteGroup(groupName);
        removeGroupFromUsers(groupName);
    }

    /**
     * removes the passed group from all users in the Artemis database, e.g. when the group was deleted
     *
     * @param groupName the group that should be removed from all existing users
     */
    public void removeGroupFromUsers(String groupName) {
        log.info("Remove group {} from users", groupName);
        List<User> users = userRepository.findAllInGroupWithAuthorities(groupName);
        log.info("Found {} users with group {}", users.size(), groupName);
        for (User user : users) {
            user.getGroups().remove(groupName);
            saveUser(user);
        }
    }

    /**
     * add the user to the specified group and update in VCS (like GitLab) if used
     *
     * @param user  the user
     * @param group the group
     * @param role the role
     */
    public void addUserToGroup(User user, String group, Role role) {
        addUserToGroupInternal(user, group); // internal Artemis database
        try {
            artemisAuthenticationProvider.addUserToGroup(user, group);  // e.g. JIRA
        }
        catch (ArtemisAuthenticationException e) {
            // This might throw exceptions, for example if the group does not exist on the authentication service. We can safely ignore it
        }
        // e.g. Gitlab: TODO: include the role to distinguish more cases
        optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(user.getLogin(), user, Set.of(), Set.of(group), false));
        optionalCIUserManagementService.ifPresent(ciUserManagementService -> ciUserManagementService.addUserToGroups(user.getLogin(), Set.of(group)));
    }

    /**
     * adds the user to the group only in the Artemis database
     *
     * @param user  the user
     * @param group the group
     */
    private void addUserToGroupInternal(User user, String group) {
        log.debug("Add user {} to group {}", user.getLogin(), group);
        if (!user.getGroups().contains(group)) {
            user.getGroups().add(group);
            user.setAuthorities(authorityService.buildAuthorities(user));
            saveUser(user);
        }
    }

    /**
     * remove the user from the specified group only in the Artemis database
     *
     * @param user  the user
     * @param group the group
     * @param role the role
     */
    public void removeUserFromGroup(User user, String group, Role role) {
        removeUserFromGroupInternal(user, group); // internal Artemis database
        artemisAuthenticationProvider.removeUserFromGroup(user, group); // e.g. JIRA
        // e.g. Gitlab
        optionalVcsUserManagementService.ifPresent(vcsUserManagementService -> vcsUserManagementService.updateVcsUser(user.getLogin(), user, Set.of(group), Set.of(), false));
        optionalCIUserManagementService.ifPresent(ciUserManagementService -> {
            ciUserManagementService.removeUserFromGroups(user.getLogin(), Set.of(group));
            ciUserManagementService.addUserToGroups(user.getLogin(), user.getGroups());
        });
    }

    /**
     * remove the user from the specified group and update in VCS (like GitLab) if used
     *
     * @param user  the user
     * @param group the group
     */
    private void removeUserFromGroupInternal(User user, String group) {
        log.info("Remove user {} from group {}", user.getLogin(), group);
        if (user.getGroups().contains(group)) {
            user.getGroups().remove(group);
            user.setAuthorities(authorityService.buildAuthorities(user));
            saveUser(user);
        }
    }

    /**
     * This method first tries to find the student in the internal Artemis user database (because the user is most probably already using Artemis).
     * In case the user cannot be found, we additionally search the (TUM) LDAP in case it is configured properly.
     *
     *       @param registrationNumber     the registration number of the user
     *       @param courseGroupName        the courseGroup the user has to be added to
     *       @param courseGroupRole        the courseGroupRole enum
     *       @param login                  the login of the user
     *       @return the found student, otherwise returns an empty optional
     *
     * */
    public Optional<User> findUserAndAddToCourse(String registrationNumber, String courseGroupName, Role courseGroupRole, String login) {
        try {
            // 1) we use the registration number and try to find the student in the Artemis user database
            var optionalStudent = userRepository.findUserWithGroupsAndAuthoritiesByRegistrationNumber(registrationNumber);
            if (optionalStudent.isPresent()) {
                var student = optionalStudent.get();
                // we only need to add the student to the course group, if the student is not yet part of it, otherwise the student cannot access the
                // course)
                if (!student.getGroups().contains(courseGroupName)) {
                    this.addUserToGroup(student, courseGroupName, courseGroupRole);
                }
                return optionalStudent;
            }

            // 2) if we cannot find the student, we use the registration number and try to find the student in the (TUM) LDAP, create it in the Artemis DB and in a
            // potential external user management system
            optionalStudent = this.createUserFromLdap(registrationNumber);
            if (optionalStudent.isPresent()) {
                var student = optionalStudent.get();
                // the newly created user needs to get the rights to access the course
                this.addUserToGroup(student, courseGroupName, courseGroupRole);
                return optionalStudent;
            }

            // 3) if we cannot find the user in the (TUM) LDAP or the registration number was not set properly, try again using the login
            optionalStudent = userRepository.findUserWithGroupsAndAuthoritiesByLogin(login);
            if (optionalStudent.isPresent()) {
                var student = optionalStudent.get();
                // the newly created user needs to get the rights to access the course
                this.addUserToGroup(student, courseGroupName, courseGroupRole);
                return optionalStudent;
            }

            log.warn("User with registration number '{}' and login '{}' not found in Artemis user database nor found in (TUM) LDAP", registrationNumber, login);
        }
        catch (Exception ex) {
            log.warn("Error while processing user with registration number " + registrationNumber, ex);
        }
        return Optional.empty();
    }
}
