import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { MomentModule } from 'ngx-moment';
import { CourseScoresComponent } from './course-scores.component';
import { ArtemisCourseScoresRoutingModule } from 'app/course/course-scores/course-scores-routing.module';

@NgModule({
    imports: [SharedModule, MomentModule, ArtemisCourseScoresRoutingModule],
    declarations: [CourseScoresComponent],
})
export class ArtemisCourseScoresModule {}
