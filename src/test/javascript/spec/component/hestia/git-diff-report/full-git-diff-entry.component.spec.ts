import { ArtemisTestModule } from '../../../test.module';
import { MockSyncStorage } from '../../../helpers/mocks/service/mock-sync-storage.service';
import { MockActivatedRoute } from '../../../helpers/mocks/activated-route/mock-activated-route';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { MockProfileService } from '../../../helpers/mocks/service/mock-profile.service';
import { FullGitDiffEntryComponent } from 'app/exercises/programming/hestia/git-diff-report/full-git-diff-entry.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { ActivatedRoute } from '@angular/router';
import { ProgrammingExerciseFullGitDiffEntry } from 'app/entities/hestia/programming-exercise-full-git-diff-entry.model';
import { AceEditorComponent } from 'app/shared/markdown-editor/ace-editor/ace-editor.component';

describe('ProgrammingExerciseFullGitDiffEntry Component', () => {
    let comp: FullGitDiffEntryComponent;
    let fixture: ComponentFixture<FullGitDiffEntryComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, TranslateModule.forRoot()],
            declarations: [FullGitDiffEntryComponent, AceEditorComponent],
            providers: [
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: ActivatedRoute, useValue: new MockActivatedRoute() },
                { provide: ProfileService, useValue: new MockProfileService() },
            ],
        }).compileComponents();
        fixture = TestBed.createComponent(FullGitDiffEntryComponent);
        comp = fixture.componentInstance;

        comp.diffEntry = new ProgrammingExerciseFullGitDiffEntry();
        comp.diffEntry.id = 123;
        comp.diffEntry.filePath = '/src/de/test.java';
        comp.diffEntry.previousLine = 1;
        comp.diffEntry.line = 10;
        comp.diffEntry.previousCode = 'ABC';
        comp.diffEntry.code = 'DEF';
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('Should setup editors', () => {
        jest.spyOn(comp.editorNow.getEditor(), 'setOptions');
        jest.spyOn(comp.editorNow.getEditor().getSession(), 'setValue');
        jest.spyOn(comp.editorPrevious.getEditor(), 'setOptions');
        jest.spyOn(comp.editorPrevious.getEditor().getSession(), 'setValue');

        comp.ngOnInit();

        expect(comp.editorNow.getEditor().setOptions).toHaveBeenCalledTimes(1);
        expect(comp.editorNow.getEditor().setOptions).toHaveBeenCalledWith({
            animatedScroll: true,
            maxLines: Infinity,
        });
        expect(comp.editorPrevious.getEditor().setOptions).toHaveBeenCalledTimes(1);
        expect(comp.editorPrevious.getEditor().setOptions).toHaveBeenCalledWith({
            animatedScroll: true,
            maxLines: Infinity,
        });

        expect(comp.editorNow.getEditor().getSession().setValue).toHaveBeenCalledTimes(1);
        expect(comp.editorNow.getEditor().getSession().setValue).toHaveBeenCalledWith('DEF');
        expect(comp.editorPrevious.getEditor().getSession().setValue).toHaveBeenCalledTimes(1);
        expect(comp.editorPrevious.getEditor().getSession().setValue).toHaveBeenCalledWith('ABC');

        expect(comp.editorNow.getEditor().container.style.background).toBe('rgba(63, 185, 80, 0.5)');
        expect(comp.editorPrevious.getEditor().container.style.background).toBe('rgba(248, 81, 73, 0.5)');
    });

    it('Should give correct line for gutter', () => {
        comp.ngOnInit();
        const gutterRendererNow = comp.editorNow.getEditor().session.gutterRenderer;
        const gutterRendererPrevious = comp.editorPrevious.getEditor().session.gutterRenderer;
        const config = { characterWidth: 1 };

        expect(gutterRendererNow.getText(null, 2)).toBe(12);
        expect(gutterRendererPrevious.getText(null, 0)).toBe(1);
        expect(gutterRendererNow.getWidth(null, 2, config)).toBe(2);
        expect(gutterRendererPrevious.getWidth(null, 0, config)).toBe(1);
    });
});
