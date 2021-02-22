package de.tum.in.www1.artemis.web.rest.lecture;

import static de.tum.in.www1.artemis.web.rest.util.ResponseUtil.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.LearningGoal;
import de.tum.in.www1.artemis.domain.Lecture;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.lecture.AttachmentUnit;
import de.tum.in.www1.artemis.domain.lecture.ExerciseUnit;
import de.tum.in.www1.artemis.domain.lecture.LectureUnit;
import de.tum.in.www1.artemis.domain.lecture.LectureUnitInteraction;
import de.tum.in.www1.artemis.repository.*;
import de.tum.in.www1.artemis.service.AuthorizationCheckService;
import de.tum.in.www1.artemis.web.rest.util.HeaderUtil;

@RestController
@RequestMapping("/api")
public class LectureUnitResource {

    private final Logger log = LoggerFactory.getLogger(LectureUnitResource.class);

    private static final String ENTITY_NAME = "lectureUnit";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AuthorizationCheckService authorizationCheckService;

    private final LectureUnitRepository lectureUnitRepository;

    private final LectureRepository lectureRepository;

    private final LearningGoalRepository learningGoalRepository;

    private final LectureUnitInteractionRepository lectureUnitInteractionRepository;

    private final UserRepository userRepository;

    public LectureUnitResource(AuthorizationCheckService authorizationCheckService, LectureRepository lectureRepository, LectureUnitRepository lectureUnitRepository,
            LearningGoalRepository learningGoalRepository, LectureUnitInteractionRepository lectureUnitInteractionRepository, UserRepository userRepository) {
        this.authorizationCheckService = authorizationCheckService;
        this.lectureUnitRepository = lectureUnitRepository;
        this.lectureRepository = lectureRepository;
        this.learningGoalRepository = learningGoalRepository;
        this.lectureUnitInteractionRepository = lectureUnitInteractionRepository;
        this.userRepository = userRepository;

    }

    @GetMapping("/lectures/{lectureId}/interactions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<LectureUnitInteraction>> getInteractionsOfUserWithLecture(@PathVariable Long lectureId) throws URISyntaxException {
        log.debug("REST request to get the interaction with lecture {}", lectureId);
        Lecture lecture = lectureRepository.findByIdElseThrow(lectureId);
        User user = userRepository.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastStudentInCourse(lecture.getCourse(), user)) {
            return forbidden();
        }
        List<LectureUnitInteraction> interactionsOfUserWithLecture = lectureUnitInteractionRepository.interactionsOfUserWithLecture(lecture, user);
        return ResponseEntity.ok().body(interactionsOfUserWithLecture);
    }

    /**
     * POST /lectures/:lectureId/lecture-units/:lectureUnitId/interactions : creates a new interaction of a student with a lecture unit
     *
     * @param lectureId     the id of the lecture to which the lecture unit belongs
     * @param lectureUnitId the id of the lecture unit to which the interaction should be added
     * @return the ResponseEntity with status 201 (Created) and with body the newly created interaction
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/lectures/{lectureId}/lecture-units/{lectureUnitId}/interactions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<LectureUnitInteraction> createInteractionWithLectureUnit(@PathVariable Long lectureId, @PathVariable Long lectureUnitId,
            @RequestBody LectureUnitInteraction interactionFromClient) throws URISyntaxException {
        log.debug("REST request to create interaction with lecture unit: {}", lectureUnitId);
        LectureUnit lectureUnit = lectureUnitRepository.findByIdElseThrow(lectureUnitId);
        if (!lectureUnit.getLecture().getId().equals(lectureId)) {
            return badRequest();
        }
        User user = userRepository.getUserWithGroupsAndAuthorities();
        if (!authorizationCheckService.isAtLeastStudentInCourse(lectureUnit.getLecture().getCourse(), user)) {
            return forbidden();
        }
        if (interactionFromClient.getId() != null) {
            return badRequest();
        }
        LectureUnitInteraction persistedInteraction = new LectureUnitInteraction(interactionFromClient.getProgressInPercent(), user, lectureUnit);
        persistedInteraction = lectureUnitInteractionRepository.saveAndFlush(persistedInteraction);

        return ResponseEntity.created(new URI("/api/lecture-unit-interactions/" + persistedInteraction.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "lectureUnitInteraction", "")).body(persistedInteraction);
    }

    /**
     * PUT /lectures/:lectureId/lecture-units-order
     *
     * @param lectureId           the id of the lecture for which to update the lecture unit order
     * @param orderedLectureUnits ordered lecture units
     * @return the ResponseEntity with status 200 (OK) and with body the ordered lecture units
     */
    @PutMapping("/lectures/{lectureId}/lecture-units-order")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<List<LectureUnit>> updateLectureUnitsOrder(@PathVariable Long lectureId, @RequestBody List<LectureUnit> orderedLectureUnits) {
        log.debug("REST request to update the order of lecture units of lecture: {}", lectureId);
        Optional<Lecture> lectureOptional = lectureRepository.findByIdWithStudentQuestionsAndLectureUnitsAndLearningGoals(lectureId);
        if (lectureOptional.isEmpty()) {
            return notFound();
        }
        Lecture lecture = lectureOptional.get();
        if (lecture.getCourse() == null) {
            return conflict();
        }
        if (!authorizationCheckService.isAtLeastInstructorInCourse(lecture.getCourse(), null)) {
            return forbidden();
        }

        // Ensure that exactly as many lecture units have been received as are currently related to the lecture
        if (orderedLectureUnits.size() != lecture.getLectureUnits().size()) {
            return conflict();
        }

        // Ensure that all received lecture units are already related to the lecture
        for (LectureUnit lectureUnit : orderedLectureUnits) {
            if (!lecture.getLectureUnits().contains(lectureUnit)) {
                return conflict();
            }
            // Set the lecture manually as it won't be included in orderedLectureUnits
            lectureUnit.setLecture(lecture);

            // keep bidirectional mapping between attachment unit and attachment
            if (lectureUnit instanceof AttachmentUnit) {
                ((AttachmentUnit) lectureUnit).getAttachment().setAttachmentUnit((AttachmentUnit) lectureUnit);
            }

        }

        lecture.setLectureUnits(orderedLectureUnits);
        Lecture persistedLecture = lectureRepository.save(lecture);
        return ResponseEntity.ok(persistedLecture.getLectureUnits());
    }

    /**
     * DELETE lectures/:lectureId/lecture-units/:lectureUnitId
     * @param lectureId the id of the lecture to which the unit belongs
     * @param lectureUnitId the id of the lecture unit to remove
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/lectures/{lectureId}/lecture-units/{lectureUnitId}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<Void> deleteLectureUnit(@PathVariable Long lectureUnitId, @PathVariable Long lectureId) {
        log.info("REST request to delete lecture unit: {}", lectureUnitId);
        Optional<LectureUnit> lectureUnitOptional = lectureUnitRepository.findByIdWithLearningGoalsBidirectional(lectureUnitId);
        if (lectureUnitOptional.isEmpty()) {
            return notFound();
        }
        LectureUnit lectureUnit = lectureUnitOptional.get();

        if (lectureUnit.getLecture() == null || lectureUnit.getLecture().getCourse() == null) {
            return conflict();
        }
        if (!lectureUnit.getLecture().getId().equals(lectureId)) {
            return conflict();
        }
        if (!authorizationCheckService.isAtLeastInstructorInCourse(lectureUnit.getLecture().getCourse(), null)) {
            return forbidden();
        }

        // we have to get the lecture from the db so that that the lecture units are included
        Optional<Lecture> lectureOptional = lectureRepository.findByIdWithStudentQuestionsAndLectureUnitsAndLearningGoals(lectureUnit.getLecture().getId());
        if (lectureOptional.isEmpty()) {
            return notFound();
        }
        Lecture lecture = lectureOptional.get();

        // update associated learning goals
        Set<LearningGoal> associatedLearningGoals = new HashSet<>(lectureUnit.getLearningGoals());
        for (LearningGoal learningGoal : associatedLearningGoals) {
            Optional<LearningGoal> learningGoalFromDbOptional = learningGoalRepository.findByIdWithLectureUnitsBidirectional(learningGoal.getId());
            if (learningGoalFromDbOptional.isPresent()) {
                LearningGoal learningGoalFromDb = learningGoalFromDbOptional.get();
                learningGoalFromDb.removeLectureUnit(lectureUnit);
                learningGoalRepository.save(learningGoalFromDb);
            }
        }

        List<LectureUnit> filteredLectureUnits = lecture.getLectureUnits();
        filteredLectureUnits.removeIf(lu -> lu.getId().equals(lectureUnitId));
        lecture.setLectureUnits(filteredLectureUnits);
        lectureRepository.save(lecture);

        String lectureUnitName;

        if (lectureUnit instanceof ExerciseUnit && ((ExerciseUnit) lectureUnit).getExercise() != null) {
            lectureUnitName = ((ExerciseUnit) lectureUnit).getExercise().getTitle();
        }
        else if (lectureUnit instanceof AttachmentUnit && ((AttachmentUnit) lectureUnit).getAttachment() != null) {
            lectureUnitName = ((AttachmentUnit) lectureUnit).getAttachment().getName();
        }
        else {
            lectureUnitName = lectureUnit.getName();
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, lectureUnitName)).build();
    }

}
