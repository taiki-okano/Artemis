import { NgModule } from '@angular/core';

import { FindLanguageFromKeyPipe } from 'app/shared/language/find-language-from-key.pipe';
import { SharedLibsModule } from 'app/shared/shared-libs.module';
import { AlertErrorComponent } from 'app/shared/alert/alert-error.component';
import { AlertComponent } from 'app/shared/alert/alert.component';

@NgModule({
    imports: [SharedLibsModule],
    declarations: [FindLanguageFromKeyPipe, AlertComponent, AlertErrorComponent],
    exports: [SharedLibsModule, FindLanguageFromKeyPipe, AlertComponent, AlertErrorComponent],
})
export class SharedCommonModule {}
