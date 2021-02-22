package de.tum.in.www1.artemis.domain.lecture;

import javax.persistence.*;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.tum.in.www1.artemis.domain.DomainObject;
import de.tum.in.www1.artemis.domain.User;

/**
 * This entity stores various properties of the interaction of students with lecture unuts
 */
@Entity
@Table(name = "lecture_unit_interaction")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LectureUnitInteraction extends DomainObject {

    /**
     * At the moment, this property is used for video, text and file units.
     * When a student checks these off as completed, a progress of 100% is saved.
     * Progress in Exercise Units, on the other hand, is gained from the student's result in the task:
     * In the future, this progress will be extended so that,for example, video progress will be measured.
     * What percentage of the video has the student already watched?
     */
    @Column(name = "progress_in_percent")
    private Double progressInPercent;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JsonIgnoreProperties("lectureUnitInteractions")
    @JoinColumn(name = "lecture_unit_id")
    private LectureUnit lectureUnit;

    public LectureUnitInteraction() {
    }

    public LectureUnitInteraction(Double progressInPercent, User student, LectureUnit lectureUnit) {
        this.progressInPercent = progressInPercent;
        this.student = student;
        this.lectureUnit = lectureUnit;
    }

    public Double getProgressInPercent() {
        return progressInPercent;
    }

    public void setProgressInPercent(Double progress) {
        this.progressInPercent = progress;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LectureUnit getLectureUnit() {
        return lectureUnit;
    }

    public void setLectureUnit(LectureUnit lectureUnit) {
        this.lectureUnit = lectureUnit;
    }
}
