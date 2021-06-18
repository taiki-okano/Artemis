import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { GuidedTourComponent } from './guided-tour.component';
import { DeviceDetectorService } from 'ngx-device-detector';

@NgModule({
    declarations: [GuidedTourComponent],
    imports: [SharedModule],
    exports: [GuidedTourComponent],
    entryComponents: [GuidedTourComponent],
    providers: [DeviceDetectorService],
})
export class GuidedTourModule {}
