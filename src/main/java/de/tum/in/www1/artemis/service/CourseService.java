package de.tum.in.www1.artemis.service;

import static de.tum.in.www1.artemis.domain.enumeration.AssessmentType.AUTOMATIC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.config.Constants;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.RepositoryType;
import de.tum.in.www1.artemis.domain.exam.Exam;
import de.tum.in.www1.artemis.domain.exam.ExerciseGroup;
import de.tum.in.www1.artemis.domain.modeling.ModelingExercise;
import de.tum.in.www1.artemis.domain.notification.GroupNotification;
import de.tum.in.www1.artemis.domain.participation.ProgrammingExerciseStudentParticipation;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.LearningGoalRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.security.ArtemisAuthenticationProvider;
import de.tum.in.www1.artemis.web.rest.dto.RepositoryExportOptionsDTO;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.EntityNotFoundException;

/**
 * Service Implementation for managing Course.
 */
@Service
public class CourseService {

    private final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final String pathToExportsFolder = "./exports";

    private final CourseRepository courseRepository;

    private final ExerciseService exerciseService;

    private final AuthorizationCheckService authCheckService;

    private final ArtemisAuthenticationProvider artemisAuthenticationProvider;

    private final UserRepository userRepository;

    private final LectureService lectureService;

    private final NotificationService notificationService;

    private ExamService examService;

    private final ExerciseGroupService exerciseGroupService;

    private final AuditEventRepository auditEventRepository;

    private final UserService userService;

    private final LearningGoalRepository learningGoalRepository;

    private final ProgrammingExerciseExportService programmingExerciseExportService;

    private final ZipFileService zipFileService;

    private final FileService fileService;

    public CourseService(CourseRepository courseRepository, ExerciseService exerciseService, AuthorizationCheckService authCheckService,
            ArtemisAuthenticationProvider artemisAuthenticationProvider, UserRepository userRepository, LectureService lectureService, NotificationService notificationService,
            ExerciseGroupService exerciseGroupService, AuditEventRepository auditEventRepository, UserService userService, LearningGoalRepository learningGoalRepository,
            ProgrammingExerciseExportService programmingExerciseExportService, ZipFileService zipFileService, FileService fileService) {
        this.courseRepository = courseRepository;
        this.exerciseService = exerciseService;
        this.authCheckService = authCheckService;
        this.artemisAuthenticationProvider = artemisAuthenticationProvider;
        this.userRepository = userRepository;
        this.lectureService = lectureService;
        this.notificationService = notificationService;
        this.exerciseGroupService = exerciseGroupService;
        this.auditEventRepository = auditEventRepository;
        this.userService = userService;
        this.learningGoalRepository = learningGoalRepository;
        this.programmingExerciseExportService = programmingExerciseExportService;
        this.zipFileService = zipFileService;
        this.fileService = fileService;
    }

    @Autowired
    // break the dependency cycle
    public void setExamService(ExamService examService) {
        this.examService = examService;
    }

    /**
     * Save a course.
     *
     * @param course the entity to save
     * @return the persisted entity
     */
    public Course save(Course course) {
        log.debug("Request to save Course : {}", course);
        return courseRepository.save(course);
    }

    /**
     * Get all the courses.
     *
     * @return the list of entities
     */
    public List<Course> findAll() {
        log.debug("Request to get all courses");
        return courseRepository.findAll();
    }

    /**
     * Get all the courses.
     *
     * @return the list of entities
     */
    public List<Course> findAllActiveWithLecturesAndExams() {
        log.debug("Request to get all active courses");
        return courseRepository.findAllActiveWithLecturesAndExams(ZonedDateTime.now());
    }

    /**
     * Get all the courses.
     *
     * @return the list of entities
     */
    public List<Course> findAllCurrentlyActiveNotOnlineAndRegistrationEnabled() {
        log.debug("Request to get all active courses which are not online and enabled");
        return courseRepository.findAllCurrentlyActiveNotOnlineAndRegistrationEnabled(ZonedDateTime.now());
    }

    /**
     * Get one course with exercises and lectures (filtered for given user)
     *
     * @param courseId the course to fetch
     * @param user     the user entity
     * @return the course including exercises and lectures for the user
     */
    public Course findOneWithExercisesAndLecturesForUser(Long courseId, User user) {
        Course course = findOneWithLecturesAndExams(courseId);
        if (!authCheckService.isAtLeastStudentInCourse(course, user)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }
        course.setExercises(exerciseService.findAllForCourse(course, user));
        course.setLectures(lectureService.filterActiveAttachments(course.getLectures(), user));
        if (authCheckService.isOnlyStudentInCourse(course, user)) {
            course.setExams(examService.filterVisibleExams(course.getExams()));
        }
        return course;
    }

    /**
     * Get all courses for the given user
     *
     * @param user the user entity
     * @return the list of all courses for the user
     */
    public List<Course> findAllActiveForUser(User user) {
        return courseRepository.findAllActive(ZonedDateTime.now()).stream().filter(course -> course.getEndDate() == null || course.getEndDate().isAfter(ZonedDateTime.now()))
                .filter(course -> isActiveCourseVisibleForUser(user, course)).collect(Collectors.toList());
    }

    /**
     * Get all courses with exercises and lectures (filtered for given user)
     *
     * @param user the user entity
     * @return the list of all courses including exercises and lectures for the user
     */
    public List<Course> findAllActiveWithExercisesAndLecturesForUser(User user) {
        return findAllActiveWithLecturesAndExams().stream()
                // filter old courses and courses the user should not be able to see
                // skip old courses that have already finished
                .filter(course -> course.getEndDate() == null || course.getEndDate().isAfter(ZonedDateTime.now())).filter(course -> isActiveCourseVisibleForUser(user, course))
                .peek(course -> {
                    course.setExercises(exerciseService.findAllForCourse(course, user));
                    course.setLectures(lectureService.filterActiveAttachments(course.getLectures(), user));
                    if (authCheckService.isOnlyStudentInCourse(course, user)) {
                        course.setExams(examService.filterVisibleExams(course.getExams()));
                    }
                }).collect(Collectors.toList());
    }

    private boolean isActiveCourseVisibleForUser(User user, Course course) {
        // Instructors and TAs see all courses that have not yet finished
        if (authCheckService.isAtLeastTeachingAssistantInCourse(course, user)) {
            return true;
        }
        // Students see all courses that have already started (and not yet finished)
        if (user.getGroups().contains(course.getStudentGroupName())) {
            return course.getStartDate() == null || course.getStartDate().isBefore(ZonedDateTime.now());
        }

        return false;
    }

    /**
     * Get one course by id.
     *
     * @param courseId the id of the entity
     * @return the entity
     */
    @NotNull
    public Course findOne(Long courseId) {
        log.debug("Request to get Course : {}", courseId);
        return courseRepository.findById(courseId).orElseThrow(() -> new EntityNotFoundException("Course with id: \"" + courseId + "\" does not exist"));
    }

    /**
     * Get one course by id.
     *
     * @param courseId the id of the entity
     * @return the entity
     */
    @NotNull
    public Course findOneWithLecturesAndExams(Long courseId) {
        log.debug("Request to get Course : {}", courseId);
        return courseRepository.findWithEagerLecturesAndExamsById(courseId).orElseThrow(() -> new EntityNotFoundException("Course with id: \"" + courseId + "\" does not exist"));
    }

    /**
     * Get one course by id with all its exercises.
     *
     * @param courseId the id of the entity
     * @return the entity
     */
    public Course findOneWithExercises(long courseId) {
        log.debug("Request to get Course : {}", courseId);
        return courseRepository.findWithEagerExercisesById(courseId);
    }

    public Course findOneWithExercisesAndLectures(long courseId) {
        log.debug("Request to get Course : {}", courseId);
        return courseRepository.findWithEagerExercisesAndLecturesById(courseId);
    }

    public Course findOneWithExercisesAndLecturesAndLectureUnitsAndLearningGoals(long courseId) {
        log.debug("Request to get Course : {}", courseId);
        return courseRepository.findWithEagerExercisesAndLecturesAndLectureUnitsAndLearningGoalsById(courseId);
    }

    /**
     * Deletes all elements associated with the course including:
     * <ul>
     *     <li>The Course</li>
     *     <li>All Exercises including:
     *      Submissions, Participations, Results, Repositories and Buildplans, see {@link ExerciseService#delete}</li>
     *     <li>All Lectures and their Attachments, see {@link LectureService#delete}</li>
     *     <li>All GroupNotifications of the course, see {@link NotificationService#deleteGroupNotification}</li>
     *     <li>All default groups created by Artemis, see {@link ArtemisAuthenticationProvider#deleteGroup}</li>
     *     <li>All Exams, see {@link ExamService#deleteById}</li>
     * </ul>
     *
     * @param course the course to be deleted
     */
    public void delete(Course course) {
        log.debug("Request to delete Course : {}", course.getTitle());

        deleteLearningGoalsOfCourse(course);
        deleteExercisesOfCourse(course);
        deleteLecturesOfCourse(course);
        deleteNotificationsOfCourse(course);
        deleteDefaultGroups(course);
        deleteExamsOfCourse(course);
        courseRepository.deleteById(course.getId());
    }

    private void deleteExamsOfCourse(Course course) {
        // delete the Exams
        List<Exam> exams = examService.findAllByCourseId(course.getId());
        for (Exam exam : exams) {
            examService.delete(exam.getId());
        }
    }

    private void deleteDefaultGroups(Course course) {
        // only delete (default) groups which have been created by Artemis before
        if (course.getStudentGroupName().equals(course.getDefaultStudentGroupName())) {
            artemisAuthenticationProvider.deleteGroup(course.getStudentGroupName());
        }
        if (course.getTeachingAssistantGroupName().equals(course.getDefaultTeachingAssistantGroupName())) {
            artemisAuthenticationProvider.deleteGroup(course.getTeachingAssistantGroupName());
        }
        if (course.getInstructorGroupName().equals(course.getDefaultInstructorGroupName())) {
            artemisAuthenticationProvider.deleteGroup(course.getInstructorGroupName());
        }
    }

    private void deleteNotificationsOfCourse(Course course) {
        List<GroupNotification> notifications = notificationService.findAllGroupNotificationsForCourse(course);
        for (GroupNotification notification : notifications) {
            notificationService.deleteGroupNotification(notification);
        }
    }

    private void deleteLecturesOfCourse(Course course) {
        for (Lecture lecture : course.getLectures()) {
            lectureService.delete(lecture);
        }
    }

    private void deleteExercisesOfCourse(Course course) {
        for (Exercise exercise : course.getExercises()) {
            exerciseService.delete(exercise.getId(), true, true);
        }
    }

    private void deleteLearningGoalsOfCourse(Course course) {
        for (LearningGoal learningGoal : course.getLearningGoals()) {
            learningGoalRepository.deleteById(learningGoal.getId());
        }
    }

    /**
     * Given a Course object, it returns the number of users enrolled in the course
     *
     * @param course - the course object we are interested in
     * @return the number of students for that course
     */
    public long countNumberOfStudentsForCourse(Course course) {
        String groupName = course.getStudentGroupName();
        return userRepository.countByGroupsIsContaining(groupName);
    }

    /**
     * If the exercise is part of an exam, retrieve the course through ExerciseGroup -> Exam -> Course.
     * Otherwise the course is already set and the id can be used to retrieve the course from the database.
     *
     * @param exercise the Exercise for which the course is retrieved
     * @return the Course of the Exercise
     */
    public Course retrieveCourseOverExerciseGroupOrCourseId(Exercise exercise) {

        if (exercise.isExamExercise()) {
            ExerciseGroup exerciseGroup = exerciseGroupService.findOneWithExam(exercise.getExerciseGroup().getId());
            exercise.setExerciseGroup(exerciseGroup);
            return exerciseGroup.getExam().getCourse();
        }
        else {
            Course course = findOne(exercise.getCourseViaExerciseGroupOrCourseMember().getId());
            exercise.setCourse(course);
            return course;
        }
    }

    /**
     * filters the passed exercises for the relevant ones that need to be manually assessed. This excludes quizzes and automatic programming exercises
     *
     * @param exercises all exercises (e.g. of a course or exercise group) that should be filtered
     * @return the filtered and relevant exercises for manual assessment
     */
    public Set<Exercise> getInterestingExercisesForAssessmentDashboards(Set<Exercise> exercises) {
        return exercises.stream().filter(exercise -> exercise instanceof TextExercise || exercise instanceof ModelingExercise || exercise instanceof FileUploadExercise
                || (exercise instanceof ProgrammingExercise && exercise.getAssessmentType() != AUTOMATIC)).collect(Collectors.toSet());
    }

    /**
     * Registers a user in a course by adding him to the student group of the course
     *
     * @param user   The user that should get added to the course
     * @param course The course to which the user should get added to
     */
    public void registerUserForCourse(User user, Course course) {
        userService.addUserToGroup(user, course.getStudentGroupName());

        final var auditEvent = new AuditEvent(user.getLogin(), Constants.REGISTER_FOR_COURSE, "course=" + course.getTitle());
        auditEventRepository.add(auditEvent);
        log.info("User " + user.getLogin() + " has successfully registered for course " + course.getTitle());
    }

    /**
     * Archives the course. The course must be over before it can be archived.
     *
     * @param course The course to archive
     */
    public void archiveCourse(Course course) {
        // Archiving a course is only possible after the course is over
        if (ZonedDateTime.now().isBefore(course.getEndDate())) {
            return;
        }

        // Used to store temporary created zip files for this course.
        var pathToCourseExportFolder = Path.of(pathToExportsFolder, course.getShortName());
        try {
            Files.createDirectories(pathToCourseExportFolder);
        }
        catch (IOException e) {
            log.info("Cannot archive course {} because the temporary directories cannot be created: {}", course.getId(), e.getMessage());
            return;
        }

        // Create a zip file for each exercise.
        var exportedExercises = archiveExercisesOfCourse(course, pathToCourseExportFolder.toString());

        try {
            // Zip all of the exercises into a big course zip file
            var pathToCourseZipFile = Path.of(pathToExportsFolder, course.getShortName() + "-archive");
            zipFileService.createZipFile(pathToCourseZipFile, exportedExercises);
            log.info("Successfully archived course {}. The archive is located at: {}", course.getId(), pathToCourseZipFile);
        }
        catch (IOException e) {
            log.info("Failed to zip the course: {}: {}", course.getId(), e.getMessage());
        }
        finally {
            // Delete the temporary directory
            fileService.scheduleForDeletion(pathToCourseExportFolder, 5);
        }
    }

    /**
     * Archives the exercises of the given course by creating a zip file for
     * each exercise.
     *
     * @param course              The course that contains the exercises to archive
     * @param pathToStoreZipFiles The path to a directory that will be used to store the zipped files.
     * @return Path to the exercise zip files,
     */
    private List<Path> archiveExercisesOfCourse(Course course, String pathToStoreZipFiles) {
        // Iterate over each exercise and export the exercise submissions of the students
        ArrayList<Path> exportedExercises = new ArrayList<>();
        ArrayList<Long> exercisesThatFailedToExport = new ArrayList<>();

        // Iterate over each exercise and zip all student submissions.
        course.getExercises().forEach(exercise -> {
            // We need this call because we need to lazy load the student participations.
            var participations = exerciseService.findOneWithStudentParticipations(exercise.getId()).getStudentParticipations();

            if (exercise instanceof ProgrammingExercise) {
                var programmingParticipations = participations.stream().map(participation -> (ProgrammingExerciseStudentParticipation) participation).collect(Collectors.toList());
                var pathToArchivedExercise = archiveProgrammingExercise((ProgrammingExercise) exercise, programmingParticipations, pathToStoreZipFiles);

                if (pathToArchivedExercise == null) {
                    exercisesThatFailedToExport.add(exercise.getId());
                }
                else {
                    exportedExercises.add(pathToArchivedExercise);
                }
            }

            // TODO: Handle archiving for other exercise types
            log.info("Skipping export of exercise: {} because it's not supported yet.", exercise.getTitle());
        });

        // Notify that we couldn't export every exercise
        if (exercisesThatFailedToExport.size() > 0) {
            var failedExerciseIds = exercisesThatFailedToExport.stream().map(String::valueOf).collect(Collectors.joining(","));
            log.info("The following exercises couldn't be exported {}", failedExerciseIds);
        }

        return exportedExercises;
    }

    /**
     * Archives a programming exercise by creating a zip file. The zip file includes all student, template, solution,
     * and tests repositories.
     *
     * @param exercise              the programming exercise
     * @param studentParticipations the student participations
     * @param pathToStoreZipFiles   The path to a directory that will be used to store the zipped files.
     * @return the path to the zip file
     */
    private Path archiveProgrammingExercise(ProgrammingExercise exercise, List<ProgrammingExerciseStudentParticipation> studentParticipations, String pathToStoreZipFiles) {
        // Export student repositories
        var zippedStudentRepos = programmingExerciseExportService.exportStudentRepositories(exercise.getId(), studentParticipations, new RepositoryExportOptionsDTO());

        // Export the template, solution, and tests repositories
        var zippedTemplateRepo = programmingExerciseExportService.exportInstructorRepositoryForExercise(exercise.getId(), RepositoryType.TEMPLATE);
        var zippedSolutionRepo = programmingExerciseExportService.exportInstructorRepositoryForExercise(exercise.getId(), RepositoryType.SOLUTION);
        var zippedTestsRepo = programmingExerciseExportService.exportInstructorRepositoryForExercise(exercise.getId(), RepositoryType.TESTS);
        var zipFilePaths = List.of(zippedStudentRepos.toPath(), zippedTemplateRepo.toPath(), zippedSolutionRepo.toPath(), zippedTestsRepo.toPath());

        try {
            // Zip the student and instructor repos together.
            var pathToZippedExercise = Path.of(pathToStoreZipFiles, exercise.getShortName());
            zipFileService.createZipFile(pathToZippedExercise, zipFilePaths);
            return pathToZippedExercise;
        }
        catch (IOException e) {
            log.info("Failed to export programming exercise {}: {}", exercise.getId(), e.getMessage());
            return null;
        }
        finally {
            // Delete the zipped repo files since we don't need those anymore.
            zipFilePaths.forEach(zipFilePath -> fileService.scheduleForDeletion(zipFilePath, 5));
        }
    }

}
