import { NgModule } from '@angular/core';
import { AboutUsComponent } from 'app/core/about-us/about-us.component';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from 'app/shared/shared.module';

@NgModule({
    declarations: [AboutUsComponent],
    imports: [
        RouterModule.forChild([
            {
                path: '',
                component: AboutUsComponent,
                data: {
                    authorities: [],
                    pageTitle: 'overview.aboutUs',
                },
            },
        ]),
        TranslateModule,
        CommonModule,
        SharedModule,
    ],
})
export class ArtemisAboutUsModule {}
