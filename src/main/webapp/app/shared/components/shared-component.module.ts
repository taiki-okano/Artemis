import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { FeatureToggleModule } from 'app/shared/feature-toggle/feature-toggle.module';
import { ConfirmAutofocusButtonComponent, ConfirmAutofocusModalComponent } from 'app/shared/components/confirm-autofocus-button.component';
import { HelpIconComponent } from 'app/shared/components/help-icon.component';
import { ButtonComponent } from 'app/shared/components/button.component';
import { CloneRepoButtonComponent } from 'app/shared/components/clone-repo-button/clone-repo-button.component';
import { ExerciseActionButtonComponent } from 'app/shared/components/exercise-action-button.component';
import { ClipboardModule } from 'ngx-clipboard';
import { CourseExamArchiveButtonComponent } from 'app/shared/components/course-exam-archive-button/course-exam-archive-button.component';
import { ArtemisMarkdownModule } from 'app/shared/markdown.module';
import { NotReleasedTagComponent } from 'app/shared/components/not-released-tag.component';
import { HelpIconComponentWithoutTranslationComponent } from 'app/shared/components/help-icon-without-translation.component';
import {SubmissionDashboardTableComponent} from "app/exercises/shared/submission-dashboard-table/submission-dashboard-table.component";
import {ArtemisResultModule} from "app/exercises/shared/result/result.module";

@NgModule({
    imports: [ArtemisSharedModule, FeatureToggleModule, ClipboardModule, ArtemisMarkdownModule],
    entryComponents: [ConfirmAutofocusModalComponent],
    declarations: [
        ButtonComponent,
        HelpIconComponent,
        HelpIconComponentWithoutTranslationComponent,
        ConfirmAutofocusButtonComponent,
        ConfirmAutofocusModalComponent,
        CloneRepoButtonComponent,
        ExerciseActionButtonComponent,
        CourseExamArchiveButtonComponent,
        NotReleasedTagComponent,
        SubmissionDashboardTableComponent,
    ],
    exports: [
        ButtonComponent,
        HelpIconComponent,
        HelpIconComponentWithoutTranslationComponent,
        ConfirmAutofocusButtonComponent,
        CloneRepoButtonComponent,
        ExerciseActionButtonComponent,
        CourseExamArchiveButtonComponent,
        NotReleasedTagComponent,
        SubmissionDashboardTableComponent,
    ],
})
export class ArtemisSharedComponentModule {}
