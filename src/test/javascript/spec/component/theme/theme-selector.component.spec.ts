import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThemeSelectorComponent } from 'app/core/theme/theme-selector/theme-selector.component';

describe('ThemeSelectorComponent', () => {
    let component: ThemeSelectorComponent;
    let fixture: ComponentFixture<ThemeSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ThemeSelectorComponent],
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ThemeSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
