package de.tum.in.www1.artemis.authentication;

import static de.tum.in.www1.artemis.domain.Authority.*;
import static de.tum.in.www1.artemis.util.ModelFactory.USER_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.time.ZonedDateTime;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationJenkinsGitlabTest;
import de.tum.in.www1.artemis.connector.GitlabRequestMockProvider;
import de.tum.in.www1.artemis.domain.Authority;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.security.ArtemisInternalAuthenticationProvider;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.security.jwt.TokenProvider;
import de.tum.in.www1.artemis.service.user.PasswordService;
import de.tum.in.www1.artemis.util.ModelFactory;
import de.tum.in.www1.artemis.web.rest.UserJWTController;
import de.tum.in.www1.artemis.web.rest.dto.LtiLaunchRequestDTO;
import de.tum.in.www1.artemis.web.rest.vm.LoginVM;

public class InternalAuthenticationIntegrationTest extends AbstractSpringIntegrationJenkinsGitlabTest {

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private ArtemisInternalAuthenticationProvider artemisInternalAuthenticationProvider;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LtiUserIdRepository ltiUserIdRepository;

    @Autowired
    private LtiOutcomeUrlRepository ltiOutcomeUrlRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private GitlabRequestMockProvider gitlabRequestMockProvider;

    private User student;

    private static final String USERNAME = "student1";

    private ProgrammingExercise programmingExercise;

    private LtiLaunchRequestDTO ltiLaunchRequest;

    @BeforeEach
    public void setUp() {
        jenkinsRequestMockProvider.enableMockingOfRequests(jenkinsServer);

        database.addUsers(1, 0, 0, 0);
        database.addCourseWithOneProgrammingExercise();
        programmingExercise = programmingExerciseRepository.findAllWithEagerParticipations().get(0);
        ltiLaunchRequest = AuthenticationIntegrationTestHelper.setupDefaultLtiLaunchRequest();
        doReturn(null).when(ltiService).verifyRequest(any());

        final var userAuthority = new Authority(Role.STUDENT.getAuthority());
        final var instructorAuthority = new Authority(Role.INSTRUCTOR.getAuthority());
        final var adminAuthority = new Authority(Role.ADMIN.getAuthority());
        final var taAuthority = new Authority(Role.TEACHING_ASSISTANT.getAuthority());
        authorityRepository.saveAll(List.of(userAuthority, instructorAuthority, adminAuthority, taAuthority));

        student = userRepository.findOneWithGroupsAndAuthoritiesByLogin(USERNAME).get();
        final var encodedPassword = passwordService.encodePassword(USER_PASSWORD);
        student.setPassword(encodedPassword);
        userRepository.save(student);
        ltiLaunchRequest.setLis_person_contact_email_primary(student.getEmail());
    }

    @AfterEach
    public void teardown() {
        database.resetDatabase();
    }

    @Test
    public void launchLtiRequest_authViaEmail_success() throws Exception {
        ltiLaunchRequest.setCustom_lookup_user_by_email(true);
        request.postForm("/api/lti/launch/" + programmingExercise.getId(), ltiLaunchRequest, HttpStatus.FOUND);

        final var user = userRepository.findAll().get(0);
        final var ltiUser = ltiUserIdRepository.findAll().get(0);
        final var ltiOutcome = ltiOutcomeUrlRepository.findAll().get(0);
        assertThat(ltiUser.getUser()).isEqualTo(user);
        assertThat(ltiUser.getLtiUserId()).isEqualTo(ltiLaunchRequest.getUser_id());
        assertThat(ltiOutcome.getUser()).isEqualTo(user);
        assertThat(ltiOutcome.getExercise()).isEqualTo(programmingExercise);
        assertThat(ltiOutcome.getUrl()).isEqualTo(ltiLaunchRequest.getLis_outcome_service_url());
        assertThat(ltiOutcome.getSourcedId()).isEqualTo(ltiLaunchRequest.getLis_result_sourcedid());

        final var updatedStudent = userRepository.findOneWithGroupsAndAuthoritiesByLogin(USERNAME).get();
        assertThat(student).isEqualTo(updatedStudent);
    }

    @Test
    @WithAnonymousUser
    public void authenticateAfterLtiRequest_success() throws Exception {
        launchLtiRequest_authViaEmail_success();

        final var auth = new TestingAuthenticationToken(student.getLogin(), USER_PASSWORD);
        final var authResponse = artemisInternalAuthenticationProvider.authenticate(auth);

        assertThat(authResponse.getCredentials().toString()).isEqualTo(student.getPassword());
        assertThat(authResponse.getPrincipal()).isEqualTo(student.getLogin());
    }

    @Test
    @WithMockUser(username = "ab12cde")
    public void registerForCourse_internalAuth_success() throws Exception {
        gitlabRequestMockProvider.enableMockingOfRequests();
        final var student = ModelFactory.generateActivatedUser("ab12cde");
        userRepository.save(student);

        final var pastTimestamp = ZonedDateTime.now().minusDays(5);
        final var futureTimestamp = ZonedDateTime.now().plusDays(5);
        var course1 = ModelFactory.generateCourse(null, pastTimestamp, futureTimestamp, new HashSet<>(), "testcourse1", "tutor", "editor", "instructor");
        course1.setRegistrationEnabled(true);
        course1 = courseRepository.save(course1);

        jenkinsRequestMockProvider.mockUpdateUserAndGroups(student.getLogin(), student, student.getGroups(), Set.of(), false);
        final var updatedStudent = request.postWithResponseBody("/api/courses/" + course1.getId() + "/register", null, User.class, HttpStatus.OK);
        assertThat(updatedStudent.getGroups()).as("User is registered for course").contains(course1.getStudentGroupName());
    }

    @Test
    @WithAnonymousUser
    public void testJWTAuthentication() throws Exception {
        LoginVM loginVM = new LoginVM();
        loginVM.setUsername(USERNAME);
        loginVM.setPassword(USER_PASSWORD);
        loginVM.setRememberMe(true);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36");

        UserJWTController.JWTToken jwtToken = request.postWithResponseBody("/api/authenticate", loginVM, UserJWTController.JWTToken.class, HttpStatus.OK, httpHeaders);
        assertThat(jwtToken.getIdToken()).as("JWT token is present").isNotNull();
        assertThat(this.tokenProvider.validateTokenForAuthority(jwtToken.getIdToken())).as("JWT token is valid").isTrue();
    }
}
