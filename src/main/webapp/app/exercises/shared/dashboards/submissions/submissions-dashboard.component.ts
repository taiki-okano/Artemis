import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { HttpResponse } from '@angular/common/http';
import { Result } from 'app/entities/result.model';
import { TextSubmissionService } from 'app/exercises/text/participate/text-submission.service';
import { getLatestSubmissionResult, Submission, SubmissionType } from 'app/entities/submission.model';
import { ExerciseService } from 'app/exercises/shared/exercise/exercise.service';
import { TextExercise } from 'app/entities/text-exercise.model';
import { ExerciseType } from 'app/entities/exercise.model';
import { TextSubmission } from 'app/entities/text-submission.model';
import { SortService } from 'app/shared/service/sort.service';
import { AccountService } from 'app/core/auth/account.service';
import { getLinkToSubmissionAssessment } from 'app/utils/navigation.utils';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { map } from 'rxjs/operators';
import {TextAssessmentService} from "app/exercises/text/assess/text-assessment.service";
import {ModelingExercise} from "app/entities/modeling-exercise.model";
import {FileUploadExercise} from "app/entities/file-upload-exercise.model";
import {ProgrammingExercise} from "app/entities/programming-exercise.model";
import { SubmissionService } from '../../submission/submission.service';

@Component({
    templateUrl: './submissions-dashboard.component.html',
})
export class SubmissionsDashboardComponent implements OnInit {

    submissionType: SubmissionType;
    exerciseType: ExerciseType;
    ExerciseType = ExerciseType;
    exercise: TextExercise;
    submissions: TextSubmission[] = [];
    filteredSubmissions: TextSubmission[] = [];
    busy = false;
    predicate = 'id';
    reverse = false;
    numberOfCorrectionrounds = 1;
    courseId: number;
    exerciseId: number;
    examId: number;
    exerciseGroupId: number;

    private cancelConfirmationText: string;

    constructor(
        private route: ActivatedRoute,
        private exerciseService: ExerciseService,
        private accountService: AccountService,
        private textSubmissionService: TextSubmissionService,
        private submissionService: SubmissionService,
        private assessmentsService: TextAssessmentService,
        private translateService: TranslateService,
        private sortService: SortService,
    ) {
        translateService.get('artemisApp.textAssessment.confirmCancel').subscribe((text) => (this.cancelConfirmationText = text));
    }

    /**
     * Life cycle hook to indicate component creation is done
     */
    async ngOnInit() {
        this.busy = true;
        this.exerciseId = Number(this.route.snapshot.paramMap.get('exerciseId'));
        this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
        if (this.route.snapshot.paramMap.has('examId')) {
            this.examId = Number(this.route.snapshot.paramMap.get('examId'));
            this.exerciseGroupId = Number(this.route.snapshot.paramMap.get('exerciseGroupId'));
        }

        this.exerciseService
            .find(this.exerciseId)
            .pipe(
                map((exerciseResponse) => {
                    switch (exerciseResponse.body!.type) {
                        case ExerciseType.TEXT:
                            return <TextExercise>exerciseResponse.body!;
                        case ExerciseType.MODELING:
                            return <ModelingExercise>exerciseResponse.body!;
                        case ExerciseType.FILE_UPLOAD:
                            return <FileUploadExercise>exerciseResponse.body!;
                        case ExerciseType.PROGRAMMING:
                            return <ProgrammingExercise>exerciseResponse.body!;
                        default:
                            throw new Error('Cannot use Submissions Dashboard with the given Exercise type.');
                    }
                }),
            )
            .subscribe((exercise) => {
                this.exercise = exercise;
                this.exerciseType = exercise.type!;
                this.getSubmissions();
                this.numberOfCorrectionrounds = this.exercise.exerciseGroup ? this.exercise!.exerciseGroup.exam!.numberOfCorrectionRoundsInExam! : 1;
                this.setPermissions();
            });
    }

    public sortRows() {
        this.sortService.sortByProperty(this.submissions, this.predicate, this.reverse);
    }

    private getSubmissions(): void {

        this.submissionService.getSubmissionsForExercise(this.exercise.id!, {submittedOnly: true}) {

        }



        this.textSubmissionService
            .getTextSubmissionsForExerciseByCorrectionRound(this.exercise.id!, { submittedOnly: true })
            .pipe(
                map((response: HttpResponse<TextSubmission[]>) =>
                    response.body!.map((submission: TextSubmission) => {
                        const tmpResult = getLatestSubmissionResult(submission);
                        if (tmpResult) {
                            // reconnect some associations
                            tmpResult!.submission = submission;
                            tmpResult!.participation = submission.participation;
                            submission.participation!.results = [tmpResult!];
                        }
                        submission.participation = submission.participation as StudentParticipation;

                        return submission;
                    }),
                ),
            )
            .subscribe((submissions: TextSubmission[]) => {
                this.submissions = submissions;
                this.filteredSubmissions = submissions;
                this.busy = false;
            });
    }

    /**
     * Update the submission filter for assessments
     * @param {Submission[]} filteredSubmissions - Submissions to be filtered for
     */
    updateFilteredSubmissions(filteredSubmissions: Submission[]) {
        this.filteredSubmissions = filteredSubmissions as TextSubmission[];
    }

    /**
     * Get the assessment type of a result
     * @param {Result} result - Result to get the assessment type for
     */
    public assessmentTypeTranslationKey(result?: Result): string {
        if (result) {
            return `artemisApp.AssessmentType.${result.assessmentType}`;
        }
        return 'artemisApp.AssessmentType.null';
    }
    private setPermissions() {
        if (this.exercise.course) {
            this.exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(this.exercise.course!);
        } else {
            this.exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(this.exercise.exerciseGroup?.exam?.course!);
        }
    }

    /**
     * Cancel the current assessment and reload the submissions to reflect the change.
     */
    cancelAssessment(submission: Submission) {
        const confirmCancel = window.confirm(this.cancelConfirmationText);
        if (confirmCancel) {
            this.assessmentsService.cancelAssessment(submission.participation!.id!, submission.id!).subscribe(() => {
                this.getSubmissions();
            });
        }
    }

    /**
     * get the link for the assessment of a specific submission of the current exercise
     */
    getAssessmentLink(participationId: number, submissionId: number) {
        return getLinkToSubmissionAssessment(this.exercise.type!, this.courseId, this.exerciseId, participationId, submissionId, this.examId, this.exerciseGroupId);
    }
}
