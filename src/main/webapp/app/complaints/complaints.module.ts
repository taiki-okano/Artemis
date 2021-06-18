import { NgModule } from '@angular/core';

import { ComplaintsComponent } from './complaints.component';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { ComplaintService } from 'app/complaints/complaint.service';
import { ComplaintInteractionsComponent } from 'app/complaints/complaint-interactions.component';
import { SharedModule } from 'app/shared/shared.module';

@NgModule({
    imports: [SharedModule, MomentModule, ClipboardModule],
    declarations: [ComplaintsComponent, ComplaintInteractionsComponent],
    exports: [ComplaintsComponent, ComplaintInteractionsComponent],
    providers: [ComplaintService],
})
export class ArtemisComplaintsModule {}
