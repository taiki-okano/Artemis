import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { GradingInstructionLinkIconComponent } from 'app/shared/grading-instruction-link-icon/grading-instruction-link-icon.component';

@NgModule({
    imports: [SharedModule],
    declarations: [GradingInstructionLinkIconComponent],
    exports: [GradingInstructionLinkIconComponent],
})
export class ArtemisGradingInstructionLinkIconModule {}
