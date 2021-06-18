import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from 'app/shared/shared.module';

import { LegalRoutingModule } from 'app/core/legal/legal-routing.module';
import { PrivacyComponent } from 'app/core/legal/privacy.component';
import { ImprintComponent } from 'app/core/legal/imprint.component';

@NgModule({
    declarations: [PrivacyComponent, ImprintComponent],
    imports: [CommonModule, SharedModule, LegalRoutingModule],
})
export class ArtemisLegalModule {}
