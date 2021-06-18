import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { ConnectionNotificationComponent } from 'app/shared/notification/connection-notification/connection-notification.component';

@NgModule({
    imports: [SharedModule],
    declarations: [ConnectionNotificationComponent],
    exports: [ConnectionNotificationComponent],
})
export class ArtemisConnectionNotificationModule {}
