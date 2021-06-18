import { NgModule } from '@angular/core';
import { TableEditableFieldComponent } from 'app/shared/table/table-editable-field.component';
import { SharedLibsModule } from 'app/shared/shared-libs.module';
import { TableEditableCheckboxComponent } from 'app/shared/table/table-editable-checkbox.component';

@NgModule({
    imports: [SharedLibsModule],
    declarations: [TableEditableFieldComponent, TableEditableCheckboxComponent],
    exports: [TableEditableFieldComponent, TableEditableCheckboxComponent],
})
export class ArtemisTableModule {}
