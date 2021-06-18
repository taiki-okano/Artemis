import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared/shared.module';
import { ColorSelectorComponent } from './color-selector.component';

@NgModule({
    imports: [SharedModule],
    declarations: [ColorSelectorComponent],
    exports: [ColorSelectorComponent],
})
export class ArtemisColorSelectorModule {}
