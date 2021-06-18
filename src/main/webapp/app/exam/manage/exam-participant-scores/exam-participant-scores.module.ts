import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { ArtemisParticipantScoresModule } from 'app/shared/participant-scores/participant-scores.module';
import { ExamParticipantScoresComponent } from './exam-participant-scores.component';

@NgModule({
    imports: [SharedModule, ArtemisParticipantScoresModule],
    declarations: [ExamParticipantScoresComponent],
})
export class ArtemisExamParticipantScoresModule {}
