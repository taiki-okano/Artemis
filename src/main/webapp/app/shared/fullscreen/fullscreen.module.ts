import { FullscreenComponent } from 'app/shared/fullscreen/fullscreen.component';
import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [SharedModule],
    declarations: [FullscreenComponent],
    exports: [FullscreenComponent],
})
export class ArtemisFullscreenModule {}
