import { ModelingEditorComponent } from './modeling-editor.component';
import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { ModelingExplanationEditorComponent } from './modeling-explanation-editor.component';

@NgModule({
    imports: [SharedModule],
    declarations: [ModelingEditorComponent, ModelingExplanationEditorComponent],
    exports: [ModelingEditorComponent, ModelingExplanationEditorComponent],
})
export class ArtemisModelingEditorModule {}
