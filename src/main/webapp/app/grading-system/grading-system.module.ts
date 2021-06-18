import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { DeviceDetectorService } from 'ngx-device-detector';
import { GradingSystemComponent } from 'app/grading-system/grading-system.component';
import { GradingSystemInfoModalComponent } from 'app/grading-system/grading-system-info-modal/grading-system-info-modal.component';

@NgModule({
    declarations: [GradingSystemComponent, GradingSystemInfoModalComponent],
    imports: [SharedModule],
    exports: [GradingSystemComponent, GradingSystemInfoModalComponent],
    entryComponents: [GradingSystemComponent],
    providers: [DeviceDetectorService],
})
export class GradingSystemModule {}
