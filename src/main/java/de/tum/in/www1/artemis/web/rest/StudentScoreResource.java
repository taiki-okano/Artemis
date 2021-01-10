package de.tum.in.www1.artemis.web.rest;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.forbidden;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import de.tum.in.www1.artemis.web.rest.dto.StudentLeaderboardDTO;
import org.hibernate.internal.util.ZonedDateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.in.www1.artemis.domain.Course;
import de.tum.in.www1.artemis.domain.Exercise;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.scores.StudentScore;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.service.CourseService;
import de.tum.in.www1.artemis.service.ExerciseService;
import de.tum.in.www1.artemis.service.StudentScoreService;
import de.tum.in.www1.artemis.service.UserService;

/**
 * REST controller for managing Rating.
 */
@Validated
@RestController
@RequestMapping("/api")
public class StudentScoreResource {

    private final Logger log = LoggerFactory.getLogger(StudentScoreResource.class);

    private final StudentScoreService studentScoreService;

    private final UserService userService;

    private final ExerciseService exerciseService;

    private final CourseService courseService;

    private final AuthorizationCheckService authCheckService;

    public StudentScoreResource(StudentScoreService studentScoreService, UserService userService, ExerciseService exerciseService, CourseService courseService,
            AuthorizationCheckService authCheckService) {
        this.studentScoreService = studentScoreService;
        this.userService = userService;
        this.exerciseService = exerciseService;
        this.courseService = courseService;
        this.authCheckService = authCheckService;
    }

    /**
     * GET /student-scores/exercise/{exerciseId} : Find StudentScores by exercise id.
     *
     * @param exerciseId id of the exercise
     * @return the ResponseEntity with status 200 (OK) and with the found student scores as body
     */
    @GetMapping("/student-scores/exercise/{exerciseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<StudentScore>> getStudentScoresForExercise(@PathVariable Long exerciseId) {
        log.debug("REST request to get student scores for exercise : {}", exerciseId);
        Exercise exercise = exerciseService.findOne(exerciseId);
        User user = userService.getUserWithGroupsAndAuthorities();

        if (!authCheckService.isAtLeastInstructorForExercise(exercise, user)) {
            return forbidden();
        }

        List<StudentScore> studentScores = studentScoreService.getStudentScoresForExercise(exercise);

        return ResponseEntity.ok(studentScores);
    }

    /**
     * GET /student-scores/course/{courseId} : Find StudentScores by course id.
     *
     * @param courseId id of the course
     * @return the ResponseEntity with status 200 (OK) and with the found student scores as body
     */
    @GetMapping("/student-scores/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<StudentScore>> getStudentScoresForCourse(@PathVariable Long courseId) {
        log.debug("REST request to get student scores for exercise : {}", courseId);
        Course course = courseService.findOneWithExercises(courseId);
        User user = userService.getUserWithGroupsAndAuthorities();

        if (!authCheckService.isAtLeastInstructorInCourse(course, user)) {
            return forbidden();
        }

        List<StudentScore> studentScores = studentScoreService.getStudentScoresForCourse(course);

        return ResponseEntity.ok(studentScores);
    }

    /**
     * GET /student-scores/exercise/{exerciseId}/student/{studentLogin} : Find StudentScores by exercise id and student login.
     *
     * @param exerciseId id of the exercise
     * @param studentLogin login of the student
     * @return the ResponseEntity with status 200 (OK) and with the found student scores as body
     */
    @GetMapping("/student-scores/exercise/{exerciseId}/student/{studentLogin}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Optional<StudentScore>> getStudentScoreForExerciseAndStudent(@PathVariable Long exerciseId, @PathVariable String studentLogin) {
        log.debug("REST request to get student score for student {} and exercise {}", studentLogin, exerciseId);
        Exercise exercise = exerciseService.findOne(exerciseId);
        User user = userService.getUserWithGroupsAndAuthorities();
        Optional<User> student = userService.getUserByLogin(studentLogin);

        if (!authCheckService.isAtLeastInstructorForExercise(exercise, user)) {
            return forbidden();
        }

        Optional<StudentScore> studentScore = studentScoreService.getStudentScoreForStudentAndExercise(student.get(), exercise);
        return ResponseEntity.ok(studentScore);
    }

    /**
     * GET /student-leaderboard/course/{courseId}/mode/{mode} : Get StudentLeaderboard for course id with mode.
     *
     * @param courseId id of the course
     * @param mode mode of the leaderboard (0: all finished exercises, 1: finished exercises in last week, 2: finished exercises in last 3 weeks)
     * @return the ResponseEntity with status 200 (OK) and with the found student scores as body
     */
    @GetMapping("/student-leaderboard/course/{courseId}/mode/{mode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR', 'TA', 'USER')")
    public ResponseEntity<List<StudentLeaderboardDTO>> getStudentLeaderboard(@PathVariable Long courseId, @PathVariable Long mode) {
        log.debug("REST request to get student leaderboard for course {} with mode {}", courseId, mode);
        Course course = courseService.findOneWithExercises(courseId);
        List<User> students = userService.findAllUsersInGroup(course.getStudentGroupName());

        Set<Exercise> exercisesFromCourse = course.getExercises();
        List<Exercise> exercises = new ArrayList<>();

        if (mode == 0) {
            for (Exercise exercise : exercisesFromCourse) {
                if (exercise.isAssessmentDueDateOver()) {
                    exercises.add(exercise);
                }
            }
        } else if (mode == 1) {
            for (Exercise exercise : exercisesFromCourse) {
                if (exercise.isAssessmentDueDateOver() && !exercise.getAssessmentDueDate().isBefore(ZonedDateTime.now().minusDays(7))) {
                    exercises.add(exercise);
                }
            }
        } else if (mode == 2) {
            for (Exercise exercise : exercisesFromCourse) {
                if (exercise.isAssessmentDueDateOver() && !exercise.getAssessmentDueDate().isBefore(ZonedDateTime.now().minusDays(21))) {
                    exercises.add(exercise);
                }
            }
        }

        List<StudentLeaderboardDTO> entries = studentScoreService.getStudentLeaderboardForExercises(students, exercises);

        User user = userService.getUserWithGroupsAndAuthorities();
        if (!authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            entries = entries.subList(0, (Math.round(entries.size() / 2) + 1));
        }

        return ResponseEntity.ok(entries);
    }
}
