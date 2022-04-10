package de.tum.in.www1.artemis.web.rest.plagiarism;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismComparison;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismStatus;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.repository.plagiarism.PlagiarismComparisonRepository;
import de.tum.in.www1.artemis.security.Role;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.plagiarism.PlagiarismService;
import de.tum.in.www1.artemis.web.rest.dto.PlagiarismComparisonStatusDTO;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;

/**
 * REST controller for managing TextExercise.
 */
@RestController
@RequestMapping("api/")
public class PlagiarismResource {

    private final CourseRepository courseRepository;

    private final AuthorizationCheckService authenticationCheckService;

    private final UserRepository userRepository;

    private final Logger log = LoggerFactory.getLogger(PlagiarismResource.class);

    private final PlagiarismComparisonRepository plagiarismComparisonRepository;

    private final PlagiarismService plagiarismService;

    /**
     * helper class for plagiarism statement update requests
     */
    public static class PlagiarismStatementDTO {

        public String statement;
    }

    public PlagiarismResource(PlagiarismComparisonRepository plagiarismComparisonRepository, CourseRepository courseRepository,
            AuthorizationCheckService authenticationCheckService, UserRepository userRepository, PlagiarismService plagiarismService) {
        this.plagiarismComparisonRepository = plagiarismComparisonRepository;
        this.courseRepository = courseRepository;
        this.authenticationCheckService = authenticationCheckService;
        this.userRepository = userRepository;
        this.plagiarismService = plagiarismService;
    }

    /**
     * Update the status of the plagiarism comparison with the given ID.
     * I.e. An editor or instructor sees a possible plagiarism case for the first time and decides if it should be further examined, or if it is not a plagiarism.
     *
     * @param comparisonId of the plagiarism comparison to update the status of
     * @param courseId the id of the course
     * @param statusDTO new status for the given comparison
     * @return the ResponseEntity with status 200 (Ok) or with status 400 (Bad Request) if the parameters are invalid
     */
    @PutMapping("courses/{courseId}/plagiarism-comparisons/{comparisonId}/status")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<Void> updatePlagiarismComparisonStatus(@PathVariable("courseId") long courseId, @PathVariable("comparisonId") long comparisonId,
            @RequestBody PlagiarismComparisonStatusDTO statusDTO) {
        log.info("REST request to update the status {} of the plagiarism comparison with id: {}", statusDTO.getStatus(), comparisonId);
        Course course = courseRepository.findByIdElseThrow(courseId);
        authenticationCheckService.checkHasAtLeastRoleInCourseElseThrow(Role.EDITOR, course, null);

        // TODO: this check can take up to a few seconds in the worst case, we should do it directly in the database
        var comparison = plagiarismComparisonRepository.findByIdWithSubmissionsStudentsElseThrow(comparisonId);
        if (!Objects.equals(comparison.getPlagiarismResult().getExercise().getCourseViaExerciseGroupOrCourseMember().getId(), courseId)) {
            throw new BadRequestAlertException("The courseId does not belong to the given comparisonId", "PlagiarismComparison", "idMismatch");
        }

        plagiarismService.updatePlagiarismComparisonStatus(comparisonId, statusDTO.getStatus());
        log.info("Finished updating the status {} of the plagiarism comparison with id: {}", statusDTO.getStatus(), comparisonId);
        return ResponseEntity.ok().body(null);
    }

    /**
     * Retrieves all plagiarismComparisons related to a course that were previously confirmed.
     *
     * @param courseId the id of the course
     * @return all plagiarism cases
     */
    @GetMapping("courses/{courseId}/plagiarism-cases")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<PlagiarismComparison<?>>> getPlagiarismComparisonsForCourse(@PathVariable long courseId) {
        log.debug("REST request to get all plagiarism cases in course with id: {}", courseId);
        Course course = courseRepository.findByIdElseThrow(courseId);
        if (!authenticationCheckService.isAtLeastInstructorInCourse(course, userRepository.getUserWithGroupsAndAuthorities())) {
            throw new AccessForbiddenException("Only instructors of this course have access to its plagiarism cases.");
        }
        var foundPlagiarismCasesForCourse = plagiarismComparisonRepository.findCasesForCourse(PlagiarismStatus.CONFIRMED, courseId);
        return ResponseEntity.ok(foundPlagiarismCasesForCourse);
    }

    /**
     * Retrieves the plagiarismComparison specified by its Id.
     * If a studentLogin is passed the comparison is anonymized
     *
     * @param courseId the id of the course
     * @param comparisonId the id of the PlagiarismComparison
     * @param studentLogin optional login of the student
     * @return the PlagiarismComparison
     * @throws AccessForbiddenException if the requesting user is not affected by the plagiarism case.
     */
    @GetMapping("courses/{courseId}/plagiarism-comparisons/{comparisonId}/for-split-view")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PlagiarismComparison<?>> getPlagiarismComparisonForEditor(@PathVariable("courseId") long courseId, @PathVariable("comparisonId") Long comparisonId,
            @RequestParam(value = "studentLogin", required = false) String studentLogin) {
        var comparisonA = plagiarismComparisonRepository.findByIdWithSubmissionsStudentsAndElementsAElseThrow(comparisonId);
        var comparisonB = plagiarismComparisonRepository.findByIdWithSubmissionsStudentsAndElementsBElseThrow(comparisonId);
        Course course = courseRepository.findByIdElseThrow(courseId);
        User user = userRepository.getUserWithGroupsAndAuthorities();

        if (!authenticationCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("Only students registered for this course can access this plagiarism comparison.");
        }
        if (!Objects.equals(comparisonA.getPlagiarismResult().getExercise().getCourseViaExerciseGroupOrCourseMember().getId(), courseId)) {
            throw new BadRequestAlertException("The courseId does not belong to the given comparisonId", "PlagiarismComparison", "idMismatch");
        }

        comparisonA.setSubmissionB(comparisonB.getSubmissionB());
        if (studentLogin != null) {
            comparisonA = this.plagiarismService.anonymizeComparisonForStudentView(comparisonA, studentLogin);
        }
        comparisonA.getSubmissionA().setPlagiarismComparison(null);
        comparisonB.getSubmissionB().setPlagiarismComparison(null);
        return ResponseEntity.ok(comparisonA);
    }
}
