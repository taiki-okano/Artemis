import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared/shared.module';
import { DifficultyPickerComponent } from 'app/exercises/shared/difficulty-picker/difficulty-picker.component';

@NgModule({
    imports: [SharedModule],
    declarations: [DifficultyPickerComponent],
    exports: [DifficultyPickerComponent],
})
export class ArtemisDifficultyPickerModule {}
