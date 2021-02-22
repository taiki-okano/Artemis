import { Component, Input } from '@angular/core';

@Component({
    selector: 'jhi-lecture-unit-check-mark',
    templateUrl: './lecture-unit-check-mark.component.html',
    styleUrls: ['./lecture-unit-check-mark.component.scss'],
})
export class LectureUnitCheckMarkComponent {
    @Input()
    isChecked = false;

    constructor() {}
}
