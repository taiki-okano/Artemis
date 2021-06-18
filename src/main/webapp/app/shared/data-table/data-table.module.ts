import { NgModule } from '@angular/core';
import { SharedModule } from 'app/shared/shared.module';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { DataTableComponent } from './data-table.component';

@NgModule({
    imports: [SharedModule, NgxDatatableModule],
    declarations: [DataTableComponent],
    exports: [DataTableComponent],
})
export class ArtemisDataTableModule {}
