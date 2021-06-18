import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared/shared.module';
import { ConfirmIconComponent } from 'app/shared/confirm-icon/confirm-icon.component';

@NgModule({
    imports: [SharedModule],
    declarations: [ConfirmIconComponent],
    exports: [ConfirmIconComponent],
})
export class ArtemisConfirmIconModule {}
