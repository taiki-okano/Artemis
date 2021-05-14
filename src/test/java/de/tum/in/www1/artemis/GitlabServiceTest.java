package de.tum.in.www1.artemis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.ProjectApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.VcsRepositoryUrl;
import de.tum.in.www1.artemis.exception.VersionControlException;
import de.tum.in.www1.artemis.service.connectors.gitlab.GitLabException;
import de.tum.in.www1.artemis.util.ModelFactory;

public class GitlabServiceTest extends AbstractSpringIntegrationJenkinsGitlabTest {

    @Value("${artemis.version-control.url}")
    private URL gitlabServerUrl;

    @BeforeEach
    public void initTestCase() {
        gitlabRequestMockProvider.enableMockingOfRequests();
    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
        gitlabRequestMockProvider.reset();
    }

    @Test
    @WithMockUser(username = "student1")
    public void testCheckIfProjectExistsFails() throws GitLabApiException {
        gitlabRequestMockProvider.mockFailToCheckIfProjectExists("project-key");
        try {
            versionControlService.checkIfProjectExists("project-key", "project-name");
        }
        catch (VersionControlException e) {
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    @Test
    @WithMockUser(username = "student1")
    public void testHealthOk() throws URISyntaxException, JsonProcessingException {
        gitlabRequestMockProvider.mockHealth("ok", HttpStatus.OK);
        var health = versionControlService.health();
        assertThat(health.getAdditionalInfo().get("url")).isEqualTo(gitlabServerUrl);
        assertThat(health.isUp()).isEqualTo(true);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testHealthNotOk() throws URISyntaxException, JsonProcessingException {
        gitlabRequestMockProvider.mockHealth("notok", HttpStatus.OK);
        var health = versionControlService.health();
        assertThat(health.getAdditionalInfo().get("url")).isEqualTo(gitlabServerUrl);
        assertThat(health.getAdditionalInfo().get("status")).isEqualTo("notok");
        assertThat(health.isUp()).isEqualTo(false);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testHealthException() throws URISyntaxException, JsonProcessingException {
        gitlabRequestMockProvider.mockHealth("ok", HttpStatus.INTERNAL_SERVER_ERROR);
        var health = versionControlService.health();
        assertThat(health.isUp()).isEqualTo(false);
        assertThat(health.getException()).isNotNull();
    }

    @Test
    @WithMockUser(username = "student1")
    public void testInvalidCloneRepoUrlThrowsException() {
        ReflectionTestUtils.setField(versionControlService, "gitlabServerUrl", null);

        Exception exception = assertThrows(GitLabException.class, () -> {
            versionControlService.getCloneRepositoryUrl(null, null);
        });

        ReflectionTestUtils.setField(versionControlService, "gitlabServerUrl", gitlabServerUrl);

        String expectedMessage = "Could not build GitLab URL";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testShouldUpdateMemberPermissionIfMemberAlreadyExists() throws MalformedURLException, GitLabApiException {
        VcsRepositoryUrl repositoryUrl = new VcsRepositoryUrl("http://gitlab/CS1JAVA1/cs1java1-artemis_admin");
        String repositoryPath = urlService.getPathFromRepositoryUrl(repositoryUrl);

        User user = ModelFactory.generateActivatedUser("userLogin");

        ProjectApi projectApi = gitlabRequestMockProvider.mockAddMemberToRepositoryUserExists(repositoryPath, user.getLogin(), false);
        versionControlService.addMemberToRepository(repositoryUrl, user);

        verify(projectApi, times(1)).updateMember(repositoryPath, 1, DEVELOPER);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testShouldThrowExceptionWhenFailToUpdateMemberPermission() throws MalformedURLException, GitLabApiException {
        VcsRepositoryUrl repositoryUrl = new VcsRepositoryUrl("http://gitlab/CS1JAVA1/cs1java1-artemis_admin");
        String repositoryPath = urlService.getPathFromRepositoryUrl(repositoryUrl);
        User user = ModelFactory.generateActivatedUser("userLogin");

        gitlabRequestMockProvider.mockAddMemberToRepositoryUserExists(repositoryPath, user.getLogin(), true);
        Exception exception = assertThrows(GitLabException.class, () -> {
            versionControlService.addMemberToRepository(repositoryUrl, user);
        });

        String expectedMessage = "Unable to set permissions for user " + user.getLogin() + ". Trying to set permission " + DEVELOPER;
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testShouldNotUpdateMemberPermissionIfMemberHasLowerAccessLevel() throws MalformedURLException, GitLabApiException {
        VcsRepositoryUrl repositoryUrl = new VcsRepositoryUrl("http://gitlab/CS1JAVA1/cs1java1-artemis_admin");
        String repositoryPath = urlService.getPathFromRepositoryUrl(repositoryUrl);

        User user = ModelFactory.generateActivatedUser("userLogin");

        ProjectApi projectApi = gitlabRequestMockProvider.mockAddMemberToRepositoryLowerAccessLevel(repositoryPath, user.getLogin());
        versionControlService.addMemberToRepository(repositoryUrl, user);

        verify(projectApi, times(1)).addMember(repositoryPath, 1, DEVELOPER);
        verify(projectApi, times(0)).updateMember(repositoryPath, 1, DEVELOPER);
    }

    @Test
    @WithMockUser(username = "student1")
    public void testShouldThrowExceptionWhenFailToAddMemberToRepository() throws MalformedURLException, GitLabApiException {
        VcsRepositoryUrl repositoryUrl = new VcsRepositoryUrl("http://gitlab/CS1JAVA1/cs1java1-artemis_admin");
        String repositoryPath = urlService.getPathFromRepositoryUrl(repositoryUrl);

        User user = ModelFactory.generateActivatedUser("userLogin");

        gitlabRequestMockProvider.mockAddMemberToRepository(repositoryPath, user.getLogin(), true);
        Exception exception = assertThrows(GitLabException.class, () -> {
            versionControlService.addMemberToRepository(repositoryUrl, user);

        });

        String expectedMessage = "Error while trying to add user to repository: " + user.getLogin() + " to repo " + repositoryUrl;
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}
