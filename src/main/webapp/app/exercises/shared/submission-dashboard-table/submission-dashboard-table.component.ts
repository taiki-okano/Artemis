import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Exercise, ExerciseType } from 'app/entities/exercise.model';
import { getFirstResultWithComplaint, Submission } from 'app/entities/submission.model';
import { AccountService } from 'app/core/auth/account.service';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { FileUploadAssessmentService } from 'app/exercises/file-upload/assess/file-upload-assessment.service';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ProgrammingAssessmentManualResultService } from 'app/exercises/programming/assess/manual-result/programming-assessment-manual-result.service';
import { ModelingAssessmentService } from 'app/exercises/modeling/assess/modeling-assessment.service';
import { TextAssessmentService } from 'app/exercises/text/assess/text-assessment.service';
import { SortService } from 'app/shared/service/sort.service';
import {ResultComponent} from "app/exercises/shared/result/result.component";

@Component({
    selector: 'jhi-submission-dashboard-table',
    templateUrl: './submission-dashboard-table.component.html',
})

/**
 * this component is used by all 4 submission pages. (_exercise_type_-assessment-dashboard)
 */
export class SubmissionDashboardTableComponent implements OnInit {
    // here we sadly need to use any, as casting does not work here
    @Input() submissions: any[]; // Submission[] | TextSubmission[] | ...
    @Input() exercise: Exercise;
    @Input() courseId: number;
    @Input() examId: number;
    @Input() exerciseGroupId: number;
    @Input() numberOfCorrectionrounds: number;
    @Input() busy: boolean;
    @Input() index: number;
    predicate = 'id';
    reverse = false;
    examMode: boolean;
    baseAssessmentRoute: string;
    baseSubmissionsRoute: string;
    @Output() onCancelConfirm = new EventEmitter();

    hasComplaint: boolean;
    private cancelConfirmationText: string;
    ExerciseType = ExerciseType;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private accountService: AccountService,
        private exerciseService: ExerciseService,
        private programmingAssessmentService: ProgrammingAssessmentManualResultService,
        private fileUploadAssessmentService: FileUploadAssessmentService,
        private modelingAssessmentService: ModelingAssessmentService,
        private textAssessmentService: TextAssessmentService,
        private translateService: TranslateService,
        private sortService: SortService,
    ) {
        translateService.get('artemisApp.assessment.messages.confirmCancel').subscribe((text) => (this.cancelConfirmationText = text));
    }

    ngOnInit(): void {
        this.submissions.forEach((submission) => {
            submission.hasComplaint = !!getFirstResultWithComplaint(submission);
        });

        if (this.examId > 0) {
            this.baseAssessmentRoute =
                `/course-management/${this.courseId}/exams/${this.examId}/exercise-groups/${this.exerciseGroupId}/` +
                `${this.exercise.type}-exercises/${this.exercise.id}/submissions/`;
            this.baseSubmissionsRoute =
                `/course-management/${this.courseId}/exams/${this.examId}/exercise-groups/${this.exerciseGroupId}/` +
                `${this.exercise.type}-exercises/${this.exercise.id}/participations/`;
        } else {
            this.baseAssessmentRoute = `/course-management/${this.courseId}/${this.exercise.type}-exercises/${this.exercise.id}/submissions/`;
            this.baseSubmissionsRoute = `/course-management/${this.courseId}/${this.exercise.type}-exercises/${this.exercise.id}/participations/`;
        }
    }



    /**
     * Cancel the current assessment and reload the submissions to reflect the change.
     */
    cancelAssessment(submission: Submission) {
        const confirmCancel = window.confirm(this.cancelConfirmationText);
        if (confirmCancel) {
            // TODO: put in one service, use the same endpoint for cancel for every exercise type.
            switch (this.exercise.type) {
                case ExerciseType.FILE_UPLOAD:
                    this.fileUploadAssessmentService.cancelAssessment(submission.id!).subscribe(() => {
                        this.onCancelConfirm.emit();
                    });
                    break;
                case ExerciseType.PROGRAMMING:
                    this.programmingAssessmentService.cancelAssessment(submission.id!).subscribe(() => {
                        this.onCancelConfirm.emit();
                    });
                    break;
                case ExerciseType.MODELING:
                    this.modelingAssessmentService.cancelAssessment(submission.id!).subscribe(() => {
                        this.onCancelConfirm.emit();
                    });
                    break;
                case ExerciseType.TEXT:
                    this.textAssessmentService.cancelAssessment(submission.participation!.id!, submission.id!).subscribe(() => {
                        this.onCancelConfirm.emit();
                    });
                    break;
            }
        }
    }

    public sortRows() {
        this.sortService.sortByProperty(this.submissions, this.predicate, this.reverse);
    }
}
