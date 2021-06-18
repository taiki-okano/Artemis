import { NgModule } from '@angular/core';

import { ModePickerComponent } from 'app/exercises/shared/mode-picker/mode-picker.component';
import { SharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [SharedModule],
    declarations: [ModePickerComponent],
    exports: [ModePickerComponent],
})
export class ArtemisModePickerModule {}
