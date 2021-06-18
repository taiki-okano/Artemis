import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TutorLeaderboardComponent } from 'app/shared/dashboards/tutor-leaderboard/tutor-leaderboard.component';
import { SharedModule } from 'app/shared/shared.module';
@NgModule({
    imports: [SharedModule, RouterModule.forChild([])],
    declarations: [TutorLeaderboardComponent],
    exports: [TutorLeaderboardComponent],
})
export class ArtemisTutorLeaderboardModule {}
