import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { SystemNotificationService } from 'app/shared/notification/system-notification/system-notification.service';

@NgModule({
    imports: [SharedModule],
    providers: [SystemNotificationService],
})
export class ArtemisSystemNotificationModule {}
