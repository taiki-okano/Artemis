import { NgModule } from '@angular/core';
import { NonProgrammingExerciseDetailCommonActionsComponent } from 'app/exercises/shared/exercise-detail-common-actions/non-programming-exercise-detail-common-actions.component';
import { SharedCommonModule } from 'app/shared/shared-common.module';
import { ArtemisExerciseScoresModule } from 'app/exercises/shared/exercise-scores/exercise-scores.module';
import { SharedModule } from 'app/shared/shared.module';
import { RouterModule } from '@angular/router';
import { ArtemisAssessmentSharedModule } from 'app/assessment/assessment-shared.module';

@NgModule({
    declarations: [NonProgrammingExerciseDetailCommonActionsComponent],
    exports: [NonProgrammingExerciseDetailCommonActionsComponent],
    imports: [SharedCommonModule, ArtemisExerciseScoresModule, SharedModule, RouterModule, ArtemisAssessmentSharedModule],
})
export class NonProgrammingExerciseDetailCommonActionsModule {}
