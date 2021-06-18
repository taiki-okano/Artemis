import { NgModule } from '@angular/core';

import { SharedModule } from 'app/shared/shared.module';
import { ArtemisColorSelectorModule } from 'app/shared/color-selector/color-selector.module';
import { TagInputModule } from 'ngx-chips';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CategorySelectorComponent } from 'app/shared/category-selector/category-selector.component';

@NgModule({
    imports: [SharedModule, ArtemisColorSelectorModule, ReactiveFormsModule, FormsModule, TagInputModule],
    declarations: [CategorySelectorComponent],
    exports: [CategorySelectorComponent],
})
export class ArtemisCategorySelectorModule {}
