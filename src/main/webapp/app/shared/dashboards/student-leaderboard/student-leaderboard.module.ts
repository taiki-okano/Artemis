import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { StudentLeaderboardComponent } from 'app/shared/dashboards/student-leaderboard/student-leaderboard.component';
import { ArtemisSharedModule } from 'app/shared/shared.module';
@NgModule({
    imports: [ArtemisSharedModule, RouterModule.forChild([])],
    declarations: [StudentLeaderboardComponent],
    exports: [StudentLeaderboardComponent],
})
export class ArtemisStudentLeaderboardModule {}
