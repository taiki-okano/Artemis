import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { PresentationScoreComponent } from 'app/exercises/shared/presentation-score/presentation-score.component';

@NgModule({
    imports: [SharedModule],
    declarations: [PresentationScoreComponent],
    exports: [PresentationScoreComponent],
})
export class ArtemisPresentationScoreModule {}
