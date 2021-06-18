import { NgModule } from '@angular/core';
import { MomentModule } from 'ngx-moment';
import { ClipboardModule } from 'ngx-clipboard';
import { SharedModule } from 'app/shared/shared.module';
import { ComplaintsForTutorComponent } from './complaints-for-tutor.component';
import { ComplaintResponseService } from 'app/complaints/complaint-response.service';
import { ComplaintService } from 'app/complaints/complaint.service';

@NgModule({
    imports: [SharedModule, MomentModule, ClipboardModule],
    declarations: [ComplaintsForTutorComponent],
    exports: [ComplaintsForTutorComponent],
    providers: [ComplaintService, ComplaintResponseService],
})
export class ArtemisComplaintsForTutorModule {}
