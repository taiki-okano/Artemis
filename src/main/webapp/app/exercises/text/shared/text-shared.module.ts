import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedLibsModule } from 'app/shared/shared-libs.module';
import { TextSelectDirective } from './text-select.directive';
import { ManualTextSelectionComponent } from './manual-text-selection/manual-text-selection.component';

@NgModule({
    imports: [CommonModule, SharedLibsModule],
    declarations: [TextSelectDirective, ManualTextSelectionComponent],
    exports: [TextSelectDirective, ManualTextSelectionComponent],
})
export class TextSharedModule {}
