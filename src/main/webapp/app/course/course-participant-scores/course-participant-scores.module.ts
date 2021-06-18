import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { CourseParticipantScoresComponent } from './course-participant-scores.component';
import { ArtemisParticipantScoresModule } from 'app/shared/participant-scores/participant-scores.module';

@NgModule({
    imports: [SharedModule, ArtemisParticipantScoresModule],
    declarations: [CourseParticipantScoresComponent],
})
export class ArtemisCourseParticipantScoresModule {}
