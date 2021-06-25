import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisAssessmentSharedModule } from 'app/assessment/assessment-shared.module';
import { ArtemisResultModule } from 'app/exercises/shared/result/result.module';
import { ModelingAssessmentModule } from 'app/exercises/modeling/assess/modeling-assessment.module';
import { AssessmentInstructionsModule } from 'app/assessment/assessment-instructions/assessment-instructions.module';
import { ArtemisMarkdownModule } from 'app/shared/markdown.module';
import { SubmissionDashboardTableComponent } from 'app/exercises/shared/assess/submission-dashboard-table.component';
import {TextAssessmentAreaComponent} from "app/exercises/text/assess/text-assessment-area/text-assessment-area.component";

@NgModule({
    imports: [
        ArtemisSharedModule,
        ArtemisResultModule,
        ArtemisAssessmentSharedModule,
        ModelingAssessmentModule,
        AssessmentInstructionsModule,
        ArtemisMarkdownModule,
    ],
    declarations: [SubmissionDashboardTableComponent],
    exports: [SubmissionDashboardTableComponent],

})
export class SubmissionDashboardTableModule {}
