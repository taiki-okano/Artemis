import { HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { of } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { ActivatedRoute, ActivatedRouteSnapshot, convertToParamMap } from '@angular/router';

import { ArtemisTestModule } from "../../test.module";
import { ExampleModelingSubmissionComponent } from "app/exercises/modeling/manage/example-modeling/example-modeling-submission.component";
import { MockSyncStorage } from '../../helpers/mocks/service/mock-sync-storage.service';
import { ExerciseService } from "app/exercises/shared/exercise/exercise.service";
import { ExampleSubmissionService } from "app/exercises/shared/example-submission/example-submission.service";
import { ModelingAssessmentService } from "app/exercises/modeling/assess/modeling-assessment.service";
import { TutorParticipationService } from "app/exercises/shared/dashboards/tutor/tutor-participation.service";
import { ModelingEditorComponent } from 'app/exercises/modeling/shared/modeling-editor.component';
import { ModelingExercise, UMLDiagramType } from "app/entities/modeling-exercise.model";
import { ExampleSubmission } from "app/entities/example-submission.model";
import { ModelingSubmission } from "app/entities/modeling-submission.model";
import { Course } from "app/entities/course.model";
import { Feedback } from "app/entities/feedback.model";
import { Result } from "app/entities/result.model";
import { UMLElement, UMLModel, ApollonEditor } from '@ls1intum/apollon';
import { MockComponent, MockProvider } from 'ng-mocks';

import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import * as sinon from 'sinon';
import { stub } from 'sinon';
import { TutorParticipation } from "app/entities/participation/tutor-participation.model";

chai.use(sinonChai);
const expected = chai.expect;

describe('ModelingExercise ExampleSubmission Component', () => {
    let comp: ExampleModelingSubmissionComponent;
    let fixture: ComponentFixture<ExampleModelingSubmissionComponent>;
    let exerciseService: ExerciseService;
    let exampleSubmissionService: ExampleSubmissionService;
    let modelingAssessmentService: ModelingAssessmentService;
    let tutorParticipationService: TutorParticipationService;
    let activatedRouteSnapshot: ActivatedRouteSnapshot;

    let modelingExercise: ModelingExercise;
    let exampleSubmission: ExampleSubmission;
    let modelingSubmission: ModelingSubmission;
    let exampleAssessment: Result;

    const EXERCISE_ID = 123;
    const EXAMPLE_SUBMISSION_ID = 456;
    const SUBMISSION_ID = 789;

    beforeEach(async () => {
        const route: ActivatedRoute = {
            snapshot: {
                paramMap: convertToParamMap({}),
                queryParamMap: convertToParamMap({}),
            },
        } as any;
        await TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            declarations: [ExampleModelingSubmissionComponent, MockComponent(ModelingEditorComponent)],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: route,
                },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                MockProvider(TranslateService),
            ],
        })
        .overrideTemplate(ExampleModelingSubmissionComponent, '')
        .compileComponents();

        fixture = TestBed.createComponent(ExampleModelingSubmissionComponent);
        comp = fixture.componentInstance;
        activatedRouteSnapshot = fixture.debugElement.injector.get(ActivatedRoute).snapshot;
        exerciseService = fixture.debugElement.injector.get(ExerciseService);
        exampleSubmissionService = fixture.debugElement.injector.get(ExampleSubmissionService);
        modelingAssessmentService = fixture.debugElement.injector.get(ModelingAssessmentService);
        tutorParticipationService = fixture.debugElement.injector.get(TutorParticipationService);
        comp.modelingEditor = TestBed.createComponent(MockComponent(ModelingEditorComponent)).componentInstance;

        const course: Course = { id: 123 } as Course;
        modelingExercise = new ModelingExercise(UMLDiagramType.ClassDiagram, course, undefined);
        modelingExercise.id = EXERCISE_ID;
        modelingExercise.course = course;
        exampleSubmission = new ExampleSubmission();
        exampleSubmission.id = EXAMPLE_SUBMISSION_ID;
        exampleSubmission.assessmentExplanation = "Example Submission Assessment Explanation";
        exampleSubmission.usedForTutorial = false;
        modelingSubmission = new ModelingSubmission();
        modelingSubmission.id = SUBMISSION_ID;
        modelingSubmission.explanationText = "Modeling Submission Explanation";
        exampleSubmission.exercise = modelingExercise;
        exampleSubmission.submission = modelingSubmission;
    });

    afterEach(function () {
        sinon.restore();
    });

    it('should correctly setup', async () => {
        // @ts-ignore
        activatedRouteSnapshot.paramMap.params = { exerciseId: EXERCISE_ID, exampleSubmissionId: EXAMPLE_SUBMISSION_ID };
        spyOn(exerciseService, 'find').and.returnValue(httpResponse(modelingExercise));
        spyOn(exampleSubmissionService, 'get').and.returnValue(httpResponse(exampleSubmission));
        spyOn(modelingAssessmentService, 'getExampleAssessment').and.returnValue(httpResponse(exampleAssessment));

        const umlModel = <UMLModel>(<unknown>{
            elements: [<UMLElement>(<unknown>{ id: 'elementId1', owner: 'ownerId1' }), <UMLElement>(<unknown>{ id: 'elementId2', owner: 'ownerId2' })],
        });
        modelingSubmission.model = JSON.stringify(umlModel);

        await comp.ngOnInit();

        expect(exerciseService.find).toHaveBeenCalledWith(EXERCISE_ID);
        expect(exampleSubmissionService.get).toHaveBeenCalledWith(EXAMPLE_SUBMISSION_ID);
        expect(modelingAssessmentService.getExampleAssessment).toHaveBeenCalledWith(EXERCISE_ID, exampleSubmission.submission?.id!);
        expected(comp.umlModel).to.deep.equal(JSON.parse(modelingSubmission.model));
        expected(comp.explanationText).to.deep.equal(modelingSubmission.explanationText);
        expected(comp.usedForTutorial).to.deep.equal(exampleSubmission.usedForTutorial);
        expected(comp.assessmentExplanation).to.deep.equal(exampleSubmission.assessmentExplanation);
        expect(comp.checkScoreBoundaries()).toBeCalled;
        // @ts-ignore
        expect(comp.loadAll).toBeCalled;
    });

    it('should correctly setup for new example submission', async () => {
        // @ts-ignore
        activatedRouteSnapshot.paramMap.params = { exerciseId: EXERCISE_ID, exampleSubmissionId: 'new' };
        spyOn(exerciseService, 'find').and.returnValue(httpResponse(modelingExercise));
        spyOn(exampleSubmissionService, 'get').and.stub();
        spyOn(modelingAssessmentService, 'getExampleAssessment').and.stub();

        await comp.ngOnInit();

        expect(exerciseService.find).toHaveBeenCalledWith(EXERCISE_ID);
        expect(exampleSubmissionService.get).toHaveBeenCalledTimes(0);
        expect(modelingAssessmentService.getExampleAssessment).toHaveBeenCalledTimes(0);
    });

    it('should correctly setup from assessment dashboard', async () => {
        // @ts-ignore
        activatedRouteSnapshot.paramMap.params = { exerciseId: EXERCISE_ID, exampleSubmissionId: EXAMPLE_SUBMISSION_ID };
        // @ts-ignore
        activatedRouteSnapshot.queryParamMap.params = { readOnly: true, toComplete: true };
        spyOn(exampleSubmissionService, 'get').and.returnValue(httpResponse(exampleSubmission));

        await comp.ngOnInit();

        expect(exampleSubmissionService.get).toHaveBeenCalledWith(EXAMPLE_SUBMISSION_ID);
        expected(comp.readOnly).to.deep.equal(true);
        expected(comp.toComplete).to.deep.equal(true);
        expected(comp.assessmentMode).to.deep.equal(true);
    });

    it('should change on feedback', () => {
        const feedbacks = [new Feedback()];
        const checkScoreBoundaries = stub(comp, 'checkScoreBoundaries');
        comp.onFeedbackChanged(feedbacks);
        expected(comp.feedbacks).to.deep.equal(feedbacks);
        expected(comp.feedbackChanged).to.deep.equal(true);
        expected(checkScoreBoundaries).to.have.been.called;
    });

    it('should update assessment explanation', () => {
        spyOn(exampleSubmissionService, 'update').and.returnValue(httpResponse(exampleSubmission));
        comp.assessmentExplanation = "Assessment Explanation";
        comp.exercise = modelingExercise;
        comp.exerciseId = EXERCISE_ID;
        comp.exampleSubmission = exampleSubmission;

        // @ts-ignore
        comp.updateAssessmentExplanation();

        expected(comp.exampleSubmission.assessmentExplanation).to.deep.equal("Assessment Explanation");
        expect(exampleSubmissionService.update).toHaveBeenCalledWith(exampleSubmission, EXERCISE_ID);
    });

    it('should show assessment without model changed', () => {
        comp.umlModel = <UMLModel>(<unknown>{
            elements: [<UMLElement>(<unknown>{ owner: 'ownerId1', id: 'elementId1' }), <UMLElement>(<unknown>{ owner: 'ownerId2', id: 'elementId2' })],
        });
        comp.modelingEditor.umlModel = <UMLModel>(<unknown>{
            elements: [<UMLElement>(<unknown>{ owner: 'ownerId3', id: 'elementId3' }), <UMLElement>(<unknown>{ owner: 'ownerId4', id: 'elementId4' })],
        });
        comp.exampleSubmission = exampleSubmission;
        comp.modelingSubmission = modelingSubmission;
        comp.showAssessment();
        // @ts-ignore
        expect(comp.modelChanged).toBeCalled;
        // @ts-ignore
        expect(comp.updateExampleModelingSubmission).toBeCalled;
        expected(comp.assessmentMode).to.deep.equal(true);
    });

    it('should show assessment without model changed', () => {
        // @ts-ignore
        const modelChanged = stub(comp, 'modelChanged');
        comp.showAssessment();
        expected(modelChanged).to.have.been.called;
        expected(comp.assessmentMode).to.deep.equal(true);
    });

    it('should create new example modeling submission', () => {
        const umlModel = <UMLModel>(<unknown>{
            elements: [<UMLElement>(<unknown>{ id: 'elementId1', owner: 'ownerId1' }), <UMLElement>(<unknown>{ id: 'elementId2', owner: 'ownerId2' })],
        });
        modelingSubmission.model = JSON.stringify(umlModel);
        comp.isNewSubmission = true;
        exampleSubmission.submission = modelingSubmission;
        comp.exampleSubmission = exampleSubmission;
        spyOn(exampleSubmissionService, 'create').and.returnValue(httpResponse(exampleSubmission));

        comp.upsertExampleModelingSubmission();

        // @ts-ignore
        expect(comp.createNewExampleModelingSubmission).toBeCalled;
        expected(comp.isNewSubmission).to.deep.equal(false);
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.modelingEditor.saveSuccessful');
        expect(exampleSubmissionService.create).toBeCalled;
    });

    it('should update example modeling submission and assessment', () => {
        comp.isNewSubmission = false;
        comp.exampleSubmission = exampleSubmission;
        comp.modelingSubmission = modelingSubmission;
        const result = new Result();
        comp.result = result;
        const feedbacks = [new Feedback()];
        comp.feedbacks = feedbacks;
        spyOn(exampleSubmissionService, 'update').and.returnValue(httpResponse(exampleSubmission));
        spyOn(modelingAssessmentService, 'saveExampleAssessment').and.returnValue(of(result));

        comp.upsertExampleModelingSubmission();
        // @ts-ignore
        expect(comp.updateExampleModelingSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.updateAssessmentExplanationAndExampleAssessment).toBeCalled;
        expected(comp.result.feedbacks).to.deep.equal(feedbacks);
        expected(comp.modelingSubmission.exampleSubmission).to.deep.equal(true);
        expected(comp.isNewSubmission).to.deep.equal(false);
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.modelingEditor.saveSuccessful');
        expect(exampleSubmissionService.update).toBeCalled;
        expect(modelingAssessmentService.saveExampleAssessment).toBeCalled;
    });

    it('should update example modeling submission and assessment without modeling submission', () => {
        comp.isNewSubmission = false;
        comp.exampleSubmission = exampleSubmission;
        spyOn(exampleSubmissionService, 'create').and.returnValue(httpResponse(exampleSubmission));
        spyOn(exampleSubmissionService, 'update').and.returnValue(httpResponse(exampleSubmission));

        comp.upsertExampleModelingSubmission();
        // @ts-ignore
        expect(comp.createNewExampleModelingSubmission).toBeCalled;
        expect(exampleSubmissionService.create).toBeCalled;
        expect(exampleSubmissionService.update).toBeCalled;
    });

    it('should update the explanation', () => {
        comp.explanationChanged("New Explanation");
        expected(comp.explanationText).to.deep.equal("New Explanation");
    });

    it('should show submission', () => {
        comp.feedbackChanged = true;
        comp.exampleSubmission = exampleSubmission;
        const saveExampleAssessment = stub(comp, 'saveExampleAssessment');
        comp.showSubmission();
        expected(saveExampleAssessment).to.have.been.called;
        expected(comp.feedbackChanged).to.deep.equal(false);
    });

    it('should show submission and set assessment mode to false', () => {
        comp.feedbackChanged = false;
        comp.showSubmission();
        expected(comp.assessmentMode).to.deep.equal(false);
    });

    it('should saveExampleSubmission with non valid assessments', () => {
        comp.assessmentsAreValid = false;
        const feedback = new Feedback();
        feedback.credits = undefined;
        comp.feedbacks = [feedback];
        comp.exercise = modelingExercise;

        comp.saveExampleAssessment();

        expect(comp.checkScoreBoundaries).toBeCalled;
        expected(comp.assessmentsAreValid).to.deep.equal(false);
        expected(comp.feedbacks[0]).to.deep.equal(feedback);
        expected(comp.invalidError).to.deep.equal('The score field must be a number and can not be empty!');
    });

    it('should saveExampleSubmission with valid assessments and different assessment explanations', () => {
        comp.exercise = modelingExercise;
        comp.assessmentsAreValid = true;
        comp.exampleSubmission = exampleSubmission;
        comp.assessmentExplanation = "Assessment Explanation";
        const feedbacks = [new Feedback()];
        comp.feedbacks = feedbacks;
        const result = new Result();

        comp.saveExampleAssessment();

        expect(comp.checkScoreBoundaries).toBeCalled;
        // @ts-ignore
        expect(comp.updateAssessmentExplanationAndExampleAssessment).toBeCalled;
    });

    it('should saveExampleSubmission with valid assessments and equal assessment explanations', () => {
        const result = new Result();
        const feedbacks = [new Feedback()];
        result.feedbacks = feedbacks;
        comp.feedbacks = feedbacks;
        spyOn(modelingAssessmentService, 'saveExampleAssessment').and.returnValue(of(result));

        comp.exercise = modelingExercise;
        comp.assessmentsAreValid = true;
        comp.exampleSubmission = exampleSubmission;
        comp.assessmentExplanation = "Assessment Explanation";
        exampleSubmission.assessmentExplanation = "Assessment Explanation";

        comp.saveExampleAssessment();

        expect(comp.checkScoreBoundaries).toBeCalled;
        // @ts-ignore
        expect(comp.updateExampleAssessment).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('modelingAssessmentEditor.messages.saveSuccessful');
        expect(modelingAssessmentService.saveExampleAssessment).toBeCalled;
    });

    it('should checkScoreBoundaries with empty feedbacks', () => {
        comp.feedbacks = [];
        comp.checkScoreBoundaries();
        expected(comp.totalScore).to.deep.equal(0);
        expected(comp.assessmentsAreValid).to.deep.equal(true);
    });

    it('should checkScoreBoundaries with non empty feedbacks with undefined credits', () => {
        const feedback = new Feedback();
        feedback.credits = undefined;
        comp.feedbacks = [feedback];
        comp.exercise = modelingExercise;
        comp.checkScoreBoundaries();
        expected(comp.invalidError).to.deep.equal('The score field must be a number and can not be empty!');
        expected(comp.assessmentsAreValid).to.deep.equal(false);
    });

    it('should checkScoreBoundaries with non empty feedbacks with NaN credits', () => {
        const feedback = new Feedback();
        feedback.credits = NaN;
        comp.feedbacks = [feedback];
        comp.exercise = modelingExercise;
        comp.checkScoreBoundaries();
        expected(comp.invalidError).to.deep.equal('The score field must be a number and can not be empty!');
        expected(comp.assessmentsAreValid).to.deep.equal(false);
    });

    it('should checkScoreBoundaries with non empty feedbacks', () => {
        comp.feedbacks = [new Feedback()];
        comp.exercise = modelingExercise;
        comp.checkScoreBoundaries();
        expected(comp.totalScore).to.deep.equal(0);
        expected(comp.assessmentsAreValid).to.deep.equal(true);
        expected(comp.invalidError).undefined;
    });

    it('should read and understood readOnly', () => {
        const tutorParticipation = new TutorParticipation();
        comp.exercise = modelingExercise;
        exampleSubmission.tutorParticipations = [tutorParticipation];
        comp.exampleSubmission = exampleSubmission;
        comp.readOnly = true;
        spyOn(tutorParticipationService, 'assessExampleSubmission').and.returnValue(httpResponse(tutorParticipation));

        comp.readAndUnderstood();

        expect(tutorParticipationService.assessExampleSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.exampleSubmission.readSuccessfully');
        expect(comp.back).toBeCalled;
        // @ts-ignore
        expect(comp.router.navigate).toBeCalled;
    });

    it('should read and understood toComplete', () => {
        const tutorParticipation = new TutorParticipation();
        comp.exercise = modelingExercise;
        exampleSubmission.tutorParticipations = [tutorParticipation];
        comp.exampleSubmission = exampleSubmission;
        comp.toComplete = true;
        spyOn(tutorParticipationService, 'assessExampleSubmission').and.returnValue(httpResponse(tutorParticipation));

        comp.readAndUnderstood();

        expect(tutorParticipationService.assessExampleSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.exampleSubmission.readSuccessfully');
        expect(comp.back).toBeCalled;
        // @ts-ignore
        expect(comp.router.navigate).toBeCalled;
    });

    it('should read and understood examMode', () => {
        const tutorParticipation = new TutorParticipation();
        comp.exercise = modelingExercise;
        exampleSubmission.tutorParticipations = [tutorParticipation];
        comp.exampleSubmission = exampleSubmission;
        comp.isExamMode = true;
        spyOn(tutorParticipationService, 'assessExampleSubmission').and.returnValue(httpResponse(tutorParticipation));

        comp.readAndUnderstood();

        expect(tutorParticipationService.assessExampleSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.exampleSubmission.readSuccessfully');
        expect(comp.back).toBeCalled;
        // @ts-ignore
        expect(comp.router.navigate).toBeCalled;
    });

    it('should read and understood', () => {
        const tutorParticipation = new TutorParticipation();
        comp.exercise = modelingExercise;
        exampleSubmission.tutorParticipations = [tutorParticipation];
        comp.exampleSubmission = exampleSubmission;
        spyOn(tutorParticipationService, 'assessExampleSubmission').and.returnValue(httpResponse(tutorParticipation));

        comp.readAndUnderstood();

        expect(tutorParticipationService.assessExampleSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.exampleSubmission.readSuccessfully');
        expect(comp.back).toBeCalled;
        // @ts-ignore
        expect(comp.router.navigate).toBeCalled;
    });

    it('should check valid assessments correctly', () => {
        const feedback = new Feedback();
        feedback.credits = 5;
        const tutorParticipation = new TutorParticipation();
        exampleSubmission.tutorParticipations = [tutorParticipation];
        comp.feedbacks = [feedback];
        comp.exercise = modelingExercise;
        comp.exampleSubmission = exampleSubmission;
        comp.exampleSubmission.submission = modelingSubmission;
        spyOn(tutorParticipationService, 'assessExampleSubmission').and.returnValue(httpResponse(tutorParticipation));

        comp.checkAssessment();

        expect(window.scroll).toBeCalled;
        expect(comp.checkScoreBoundaries).toBeCalled;
        expect(tutorParticipationService.assessExampleSubmission).toBeCalled;
        // @ts-ignore
        expect(comp.jhiAlertService.success).toBeCalledWith('artemisApp.exampleSubmission.assessScore.success');
    });

    it('should check non valid assessments correctly', () => {
        const feedback = new Feedback();
        feedback.credits = undefined;
        comp.feedbacks = [feedback];

        comp.checkAssessment();

        expect(window.scroll).toBeCalled;
        expect(comp.checkScoreBoundaries).toBeCalled;
    });

    const httpResponse = (body: any) => of(new HttpResponse({ body }));
})
