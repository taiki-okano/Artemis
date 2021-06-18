import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared/shared.module';
import { SidePanelComponent } from './side-panel.component';

@NgModule({
    imports: [SharedModule],
    declarations: [SidePanelComponent],
    exports: [SidePanelComponent],
})
export class ArtemisSidePanelModule {}
