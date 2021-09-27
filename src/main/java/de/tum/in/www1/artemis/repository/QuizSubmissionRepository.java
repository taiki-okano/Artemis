package de.tum.in.www1.artemis.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.quiz.QuizSubmission;

/**
 * Spring Data JPA repository for the QuizSubmission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    /**
     * method to find orphan submissions
     * Note: type is set to fetch explicitly to make this query faster
     * @return all submissions without a parent participation and not used as example submissions
     */
    @EntityGraph(type = FETCH, attributePaths = "results")
    Set<QuizSubmission> findByParticipationIsNull();
}
