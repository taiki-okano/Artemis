package de.tum.in.www1.artemis.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.tum.in.www1.artemis.domain.Lecture;
import de.tum.in.www1.artemis.domain.User;
import de.tum.in.www1.artemis.domain.lecture.LectureUnitInteraction;

@Repository
public interface LectureUnitInteractionRepository extends JpaRepository<LectureUnitInteraction, Long> {

    @Query("""
            SELECT i
            FROM LectureUnitInteraction i JOIN i.lectureUnit lu JOIN lu.lecture l JOIN i.student s
            WHERE l = :lecture AND s = :user
            """)
    List<LectureUnitInteraction> interactionsOfUserWithLecture(@Param("lecture") Lecture lecture, @Param("user") User user);
}
