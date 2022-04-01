package de.tum.in.www1.artemis.lecture.service;

import de.tum.in.www1.artemis.domain.LearningGoal;
import de.tum.in.www1.artemis.domain.Lecture;
import de.tum.in.www1.artemis.domain.lecture.ExerciseUnit;
import de.tum.in.www1.artemis.domain.lecture.LectureUnit;
import de.tum.in.www1.artemis.repository.ExerciseUnitRepository;
import de.tum.in.www1.artemis.repository.LearningGoalRepository;
import de.tum.in.www1.artemis.repository.LectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing lecture units.
 */
@Service
public class LectureUnitService {

    private final LectureRepository lectureRepository;

    private final LearningGoalRepository learningGoalRepository;

    private final ExerciseUnitRepository exerciseUnitRepository;

    public LectureUnitService(LectureRepository lectureRepository, LearningGoalRepository learningGoalRepository, ExerciseUnitRepository exerciseUnitRepository) {
        this.lectureRepository = lectureRepository;
        this.learningGoalRepository = learningGoalRepository;
        this.exerciseUnitRepository = exerciseUnitRepository;
    }

    /**
     * Deletes a lecture unit correctly in the database
     *
     * @param lectureUnit lecture unit to delete
     */
    @Transactional
    public void removeLectureUnit(LectureUnit lectureUnit) {
        if (Objects.isNull(lectureUnit)) {
            return;
        }
        // update associated learning goals
        Set<LearningGoal> associatedLearningGoals = new HashSet<>(lectureUnit.getLearningGoals());
        for (LearningGoal learningGoal : associatedLearningGoals) {
            disconnectLectureUnitAndLearningGoal(lectureUnit, learningGoal);
        }
        Lecture lecture = lectureRepository.findByIdWithPostsAndLectureUnitsAndLearningGoalsElseThrow(lectureUnit.getLecture().getId());
        // Creating a new list of lecture units without the one we want to remove
        List<LectureUnit> lectureUnitsUpdated = new ArrayList<>();
        for (LectureUnit unit : lecture.getLectureUnits()) {
            if (Objects.nonNull(unit) && !unit.getId().equals(lectureUnit.getId())) {
                lectureUnitsUpdated.add(unit);
            }
        }
        lecture.getLectureUnits().clear();
        lecture.getLectureUnits().addAll(lectureUnitsUpdated);
        lectureRepository.save(lecture);
    }

    /**
     * Deletes exercise units from the database
     *
     * @param exerciseId the id of the exercise
     */
    @Transactional
    public void removeExerciseUnitsByExerciseId(long exerciseId) {
        List<ExerciseUnit> exerciseUnits = this.exerciseUnitRepository.findByIdWithLearningGoalsBidirectional(exerciseId);
        for (ExerciseUnit exerciseUnit : exerciseUnits) {
            removeLectureUnit(exerciseUnit);
        }
    }

    /**
     * Remove connection between lecture unit and learning goal in the database
     *
     * @param lectureUnit  Lecture unit connected to learning goal
     * @param learningGoal Learning goal connected to lecture unit
     */
    public void disconnectLectureUnitAndLearningGoal(LectureUnit lectureUnit, LearningGoal learningGoal) {
        Optional<LearningGoal> learningGoalFromDbOptional = learningGoalRepository.findByIdWithLectureUnitsBidirectional(learningGoal.getId());
        if (learningGoalFromDbOptional.isPresent()) {
            LearningGoal learningGoalFromDb = learningGoalFromDbOptional.get();
            learningGoalFromDb.removeLectureUnit(lectureUnit);
            learningGoalRepository.save(learningGoalFromDb);
        }
    }
}