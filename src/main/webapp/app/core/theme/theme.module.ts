import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { ThemeSelectorComponent } from './theme-selector/theme-selector.component';

@NgModule({
    declarations: [ThemeSelectorComponent],
    imports: [TranslateModule, CommonModule, ArtemisSharedModule],
})
export class ThemeModule {}
