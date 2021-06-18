import { NgModule } from '@angular/core';
import { ProgressBarComponent } from 'app/shared/dashboards/tutor-participation-graph/progress-bar/progress-bar.component';
import { TutorParticipationGraphComponent } from 'app/shared/dashboards/tutor-participation-graph/tutor-participation-graph.component';
import { SharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [SharedModule],
    declarations: [ProgressBarComponent, TutorParticipationGraphComponent],
    exports: [TutorParticipationGraphComponent, ProgressBarComponent],
})
export class ArtemisTutorParticipationGraphModule {}
