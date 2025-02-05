import { Component, Input, OnInit } from '@angular/core';
import { StatisticsService } from 'app/shared/statistics-graph/statistics.service';
import { TranslateService } from '@ngx-translate/core';
import { GraphColors } from 'app/entities/statistics.model';
import { AggregatedExerciseGroupResult } from 'app/exam/exam-scores/exam-score-dtos.model';
import { LocaleConversionService } from 'app/shared/service/locale-conversion.service';
import { roundValueSpecifiedByCourseSettings } from 'app/shared/util/utils';
import { ActivatedRoute, Router } from '@angular/router';
import { ExerciseType } from 'app/entities/exercise.model';
import { navigateToExamExercise } from 'app/utils/navigation.utils';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { Course } from 'app/entities/course.model';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import { NgxChartsSingleSeriesDataEntry } from 'app/shared/chart/ngx-charts-datatypes';

type NameToValueMap = { [name: string]: any };

@Component({
    selector: 'jhi-exam-scores-average-scores-graph',
    templateUrl: './exam-scores-average-scores-graph.component.html',
})
export class ExamScoresAverageScoresGraphComponent implements OnInit {
    @Input() averageScores: AggregatedExerciseGroupResult;

    courseId: number;
    course?: Course;
    examId: number;

    // ngx
    ngxData: NgxChartsSingleSeriesDataEntry[] = [];
    ngxColor = {
        name: 'exercise groups',
        selectable: true,
        group: ScaleType.Ordinal,
        domain: [],
    } as Color;
    xScaleMax = 100;
    lookup: NameToValueMap = {};

    constructor(
        private router: Router,
        private activatedRoute: ActivatedRoute,
        private service: StatisticsService,
        private translateService: TranslateService,
        private localeConversionService: LocaleConversionService,
        private courseService: CourseManagementService,
    ) {}

    ngOnInit(): void {
        this.activatedRoute.params.subscribe((params) => {
            this.courseId = +params['courseId'];
            this.examId = +params['examId'];
        });
        this.courseService.find(this.courseId).subscribe((courseResponse) => (this.course = courseResponse.body!));
        this.initializeChart();
    }

    private initializeChart(): void {
        this.lookup[this.averageScores.title] = { absoluteValue: this.averageScores.averagePoints! };
        const exerciseGroupAverage = this.averageScores.averagePercentage ? this.averageScores.averagePercentage : 0;
        this.ngxData.push({ name: this.averageScores.title, value: exerciseGroupAverage });
        this.ngxColor.domain.push(this.determineColor(true, exerciseGroupAverage));
        this.xScaleMax = this.xScaleMax > exerciseGroupAverage ? this.xScaleMax : exerciseGroupAverage;
        this.averageScores.exerciseResults.forEach((exercise) => {
            const exerciseAverage = exercise.averagePercentage ?? 0;
            this.xScaleMax = this.xScaleMax > exerciseAverage ? this.xScaleMax : exerciseAverage;
            this.ngxData.push({ name: exercise.exerciseId + ' ' + exercise.title, value: exerciseAverage });
            this.lookup[exercise.exerciseId + ' ' + exercise.title] = {
                absoluteValue: exercise.averagePoints ?? 0,
                exerciseId: exercise.exerciseId,
                exerciseType: exercise.exerciseType,
            };
            this.ngxColor.domain.push(this.determineColor(false, exerciseAverage));
        });

        this.ngxData = [...this.ngxData];
    }

    roundAndPerformLocalConversion(points: number | undefined) {
        return this.localeConversionService.toLocaleString(roundValueSpecifiedByCourseSettings(points, this.course), this.course!.accuracyOfScores!);
    }

    /**
     * We navigate to the exercise scores page when the user clicks on a data point
     */
    navigateToExercise(exerciseId: number, exerciseType: ExerciseType) {
        navigateToExamExercise(this.router, this.courseId, this.examId, this.averageScores.exerciseGroupId, exerciseType, exerciseId, 'scores');
    }

    /**
     * Appends to every xAxis tick a percentage sign
     * @param tick the initial xiAxis tick
     */
    formatTicks(tick: string): string {
        return tick + '%';
    }

    /**
     * Looks up the absolute average points of an exercise group or an exercise
     * @param name name of the exercise or exercise type
     * @returns locale string representation of the points
     */
    lookupAbsoluteValue(name: string): string {
        return this.roundAndPerformLocalConversion(this.lookup[name].absoluteValue);
    }

    /**
     * Delegates the user to the scores page of the specific exam exercise if the corresponding bar is clicked
     * @param event the event that is fired by ngx-charts
     */
    onSelect(event: any) {
        const id = this.lookup[event.name].exerciseId;
        const type = this.lookup[event.name].exerciseType;
        if (id && type) {
            this.navigateToExercise(id, type);
        }
    }

    /**
     * Determines the color for a given bar
     * @param isExerciseGroup boolean that indicates if the currently determined color is assigned to a bar representing the exercise group average
     * This is necessary because we have a color difference between the exercise group average representation and an exercise average representation
     * @param score the score the bar will represent
     * @private
     */
    private determineColor(isExerciseGroup: boolean, score: number): string {
        if (score >= 50) {
            return isExerciseGroup ? GraphColors.BLUE : GraphColors.DARK_BLUE;
        } else if (score > 25) {
            return GraphColors.YELLOW;
        } else {
            return GraphColors.RED;
        }
    }
}
