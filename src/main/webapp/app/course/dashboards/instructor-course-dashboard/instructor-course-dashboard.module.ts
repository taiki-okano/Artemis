import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ChartsModule } from 'ng2-charts';
import { instructorCourseDashboardRoute } from './instructor-course-dashboard.route';
import { InstructorCourseDashboardComponent } from './instructor-course-dashboard.component';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { ArtemisTutorLeaderboardModule } from 'app/shared/dashboards/tutor-leaderboard/tutor-leaderboard.module';
import { SharedModule } from 'app/shared/shared.module';

const ENTITY_STATES = instructorCourseDashboardRoute;

@NgModule({
    imports: [SharedModule, MomentModule, ClipboardModule, RouterModule.forChild(ENTITY_STATES), ChartsModule, ArtemisTutorLeaderboardModule],
    declarations: [InstructorCourseDashboardComponent],
})
export class ArtemisInstructorCourseStatsDashboardModule {}
