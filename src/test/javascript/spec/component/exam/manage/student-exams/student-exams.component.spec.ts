import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import * as sinon from 'sinon';
import * as moment from 'moment';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router, RouterModule } from '@angular/router';
import { StudentExamsComponent } from 'app/exam/manage/student-exams/student-exams.component';
import { ExamManagementService } from 'app/exam/manage/exam-management.service';
import { MockComponent, MockModule, MockPipe } from 'ng-mocks';
import { StudentExamService } from 'app/exam/manage/student-exams/student-exam.service';
import { CourseManagementService } from 'app/course/manage/course-management.service';
import { JhiAlertService } from 'ng-jhipster';
import { TranslateService } from '@ngx-translate/core';
import { StudentExamStatusComponent } from 'app/exam/manage/student-exams/student-exam-status.component';
import { AlertComponent } from 'app/shared/alert/alert.component';
import { ArtemisDurationFromSecondsPipe } from 'app/shared/pipes/artemis-duration-from-seconds.pipe';
import { ArtemisDatePipe } from 'app/shared/pipes/artemis-date.pipe';
import { LocalStorageService, SessionStorageService } from 'ngx-webstorage';
import { Course } from 'app/entities/course.model';
import { of, throwError } from 'rxjs';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { StudentExam } from 'app/entities/student-exam.model';
import { Exam } from 'app/entities/exam.model';
import { User } from 'app/core/user/user.model';
import { By } from '@angular/platform-browser';
import { NgbModal, NgbModalRef, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { ArtemisTestModule } from '../../../../test.module';
import { MockSyncStorage } from '../../../../helpers/mocks/service/mock-sync-storage.service';
import { AccountService } from 'app/core/auth/account.service';
import { MockAccountService } from '../../../../helpers/mocks/service/mock-account.service';
import { MockTranslateService } from '../../../../helpers/mocks/service/mock-translate.service';
import { MockNgbModalService } from '../../../../helpers/mocks/service/mock-ngb-modal.service';
import { MockRouter } from '../../../../helpers/mocks/service/mock-route.service';
import { ArtemisDataTableModule } from 'app/shared/data-table/data-table.module';
import { NgxDatatableModule } from '@swimlane/ngx-datatable';
import { SinonSpy, SinonStub } from 'sinon';

chai.use(sinonChai);
const expect = chai.expect;

describe('StudentExamsComponent', () => {
    let studentExamsComponentFixture: ComponentFixture<StudentExamsComponent>;
    let studentExamsComponent: StudentExamsComponent;

    let examManagementService: ExamManagementService;
    let studentExamService: StudentExamService;
    let courseManagementService: CourseManagementService;
    let modalService: NgbModal;
    let alertService: JhiAlertService;

    let findCourseSpy: SinonStub;
    let findExamSpy: SinonStub;
    let modalServiceOpenStub: SinonStub;
    let lockAllRepositoriesStub: SinonStub;
    let unlockAllRepositoriesStub: SinonStub;
    let assessStub: SinonStub;
    let evaluateQuizExercisesStub: SinonStub;
    let findAllStudentExamsStub: SinonStub;
    let generateStudentExamsStub: SinonStub;
    let generateMissingStudentExamsStub: SinonStub;
    let startExercisesStub: SinonStub;
    let alertServiceSpy: SinonSpy;

    let course: Course;
    let studentOne: User;
    let studentTwo: User;
    let studentExamOne: StudentExam;
    let studentExamTwo: StudentExam;
    let exam: Exam;

    beforeEach(async () => {
        course = new Course();
        course.id = 1;

        studentOne = new User();
        studentOne.id = 1;

        studentTwo = new User();
        studentTwo.id = 2;

        exam = new Exam();
        exam.course = course;
        exam.id = 1;
        exam.registeredUsers = [studentOne, studentTwo];
        exam.endDate = moment();
        exam.startDate = exam.endDate.subtract(60, 'seconds');

        studentExamOne = new StudentExam();
        studentExamOne.exam = exam;
        studentExamOne.id = 1;
        studentExamOne.workingTime = 70;
        studentExamOne.user = studentOne;

        studentExamTwo = new StudentExam();
        studentExamTwo.exam = exam;
        studentExamTwo.id = 1;
        studentExamTwo.workingTime = 70;
        studentExamTwo.user = studentOne;

        const route = { snapshot: { paramMap: convertToParamMap({ courseId: course.id, examId: exam.id }) } } as any as ActivatedRoute;

        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, ArtemisDataTableModule, MockModule(NgxDatatableModule), MockModule(RouterModule), MockModule(NgbModule)],
            declarations: [
                StudentExamsComponent,
                MockComponent(StudentExamStatusComponent),
                MockComponent(AlertComponent),
                MockPipe(ArtemisDurationFromSecondsPipe),
                MockPipe(ArtemisDatePipe),
                MockPipe(ArtemisTranslatePipe),
            ],
            providers: [
                { provide: ActivatedRoute, useValue: route },
                { provide: LocalStorageService, useClass: MockSyncStorage },
                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: AccountService, useClass: MockAccountService },
                { provide: Router, useClass: MockRouter },
                { provide: NgbModal, useClass: MockNgbModalService },
            ],
        }).compileComponents();

        studentExamsComponentFixture = TestBed.createComponent(StudentExamsComponent);
        studentExamsComponent = studentExamsComponentFixture.componentInstance;

        modalService = TestBed.inject(NgbModal);
        examManagementService = TestBed.inject(ExamManagementService);
        studentExamService = TestBed.inject(StudentExamService);
        courseManagementService = TestBed.inject(CourseManagementService);
        alertService = TestBed.inject(JhiAlertService);

        findCourseSpy = sinon.stub(examManagementService, 'find');
        findCourseSpy.returns(of(new HttpResponse<Exam>({ body: exam, status: 200 })));

        assessStub = sinon.stub(examManagementService, 'assessUnsubmittedAndEmptyStudentExams');
        assessStub.returns(of(new HttpResponse<number>({ body: 1, status: 200 })));

        generateStudentExamsStub = sinon.stub(examManagementService, 'generateStudentExams');
        generateStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne, studentExamTwo], status: 200 })));

        const missingStudentExamResponse = new HttpResponse<StudentExam[]>({ body: studentExamTwo ? [studentExamTwo] : [], status: 200 });
        generateMissingStudentExamsStub = sinon.stub(examManagementService, 'generateMissingStudentExams');
        generateMissingStudentExamsStub.returns(of(missingStudentExamResponse));

        startExercisesStub = sinon.stub(examManagementService, 'startExercises');
        startExercisesStub.returns(of(new HttpResponse<number>({ body: 2, status: 200 })));

        unlockAllRepositoriesStub = sinon.stub(examManagementService, 'unlockAllRepositories');
        unlockAllRepositoriesStub.returns(of(new HttpResponse<number>({ body: 2, status: 200 })));

        lockAllRepositoriesStub = sinon.stub(examManagementService, 'lockAllRepositories').returns(of(new HttpResponse<number>({ body: 2, status: 200 })));
        lockAllRepositoriesStub.returns(of(new HttpResponse<number>({ body: 1, status: 200 })));

        evaluateQuizExercisesStub = sinon.stub(examManagementService, 'evaluateQuizExercises').returns(of(new HttpResponse<number>({ body: 2, status: 200 })));
        evaluateQuizExercisesStub.returns(of(new HttpResponse<number>({ body: 1, status: 200 })));

        findAllStudentExamsStub = sinon.stub(studentExamService, 'findAllForExam');
        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne, studentExamTwo], status: 200 })));

        findExamSpy = sinon.stub(courseManagementService, 'find').returns(of(new HttpResponse<Course>({ body: course, status: 200 })));

        const componentInstance = { title: String, text: String };
        const result = new Promise((resolve) => resolve(true));
        modalServiceOpenStub = sinon.stub(modalService, 'open').returns(<NgbModalRef>{
            componentInstance,
            result,
        });

        alertServiceSpy = sinon.spy(alertService, 'error');
    });

    afterEach(() => {
        modalServiceOpenStub.restore();
        sinon.restore();
    });

    it('should initialize', fakeAsync(() => {
        studentExamsComponentFixture.detectChanges();

        expect(studentExamsComponentFixture).to.be.ok;
        expect(findCourseSpy).to.have.been.calledOnce;
        expect(findExamSpy).to.have.been.calledOnce;
        expect(findAllStudentExamsStub).to.have.been.calledOnce;
        expect(studentExamsComponent.course).to.deep.equal(course);
        expect(studentExamsComponent.studentExams).to.deep.equal([studentExamOne, studentExamTwo]);
        expect(studentExamsComponent.exam).to.deep.equal(exam);
        expect(studentExamsComponent.hasStudentsWithoutExam).to.equal(false);
        expect(studentExamsComponent.longestWorkingTime).to.equal(studentExamOne.workingTime);
        expect(studentExamsComponent.isExamOver).to.equal(false);
        expect(studentExamsComponent.isLoading).to.equal(false);
        tick();
    }));

    it('should not show assess unsubmitted student exam modeling and text participations', fakeAsync(() => {
        // user is not an instructor
        studentExamsComponentFixture.detectChanges();
        const assessButton = studentExamsComponentFixture.debugElement.query(By.css('#assessUnsubmittedExamModelingAndTextParticipationsButton'));
        expect(assessButton).to.not.exist;
        tick();
    }));

    it('should disable show assess unsubmitted student exam modeling and text participations', fakeAsync(() => {
        course.isAtLeastInstructor = true;

        // exam is not over
        studentExamsComponentFixture.detectChanges();
        const assessButton = studentExamsComponentFixture.debugElement.query(By.css('#assessUnsubmittedExamModelingAndTextParticipationsButton'));
        expect(assessButton).to.exist;
        expect(assessButton.nativeElement.disabled).to.equal(true);
        tick();
    }));

    it('should automatically assess modeling and text exercises of unsubmitted student exams', fakeAsync(() => {
        studentExamOne.workingTime = 10;
        exam.startDate = moment().subtract(200, 'seconds');
        exam.endDate = moment().subtract(100, 'seconds');
        exam.gracePeriod = 0;
        course.isAtLeastInstructor = true;

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamOver).to.equal(true);
        expect(course).to.exist;
        const assessButton = studentExamsComponentFixture.debugElement.query(By.css('#assessUnsubmittedExamModelingAndTextParticipationsButton'));
        expect(assessButton).to.exist;
        assessButton.nativeElement.click();
        expect(assessStub).to.have.been.calledOnce;
        tick();
    }));

    it('should correctly catch HTTPError when assessing unsubmitted exams', fakeAsync(() => {
        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        assessStub.returns(throwError(httpError));
        studentExamOne.workingTime = 10;
        exam.startDate = moment().subtract(200, 'seconds');
        exam.endDate = moment().subtract(100, 'seconds');
        exam.gracePeriod = 0;
        course.isAtLeastInstructor = true;

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamOver).to.equal(true);
        expect(course).to.exist;
        const assessButton = studentExamsComponentFixture.debugElement.query(By.css('#assessUnsubmittedExamModelingAndTextParticipationsButton'));
        expect(assessButton).to.exist;
        assessButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should generate student exams if there are none', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [], status: 200 })));
        studentExamsComponentFixture.detectChanges();
        studentExamsComponent.ngOnInit();

        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamStarted).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;

        const generateStudentExamsButton = studentExamsComponentFixture.debugElement.query(By.css('#generateStudentExamsButton'));
        expect(generateStudentExamsButton).to.exist;
        expect(generateStudentExamsButton.nativeElement.disabled).to.equal(false);
        expect(studentExamsComponent.studentExams.length).to.equal(0);

        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne, studentExamTwo], status: 200 })));
        generateStudentExamsButton.nativeElement.click();
        expect(generateStudentExamsStub).to.have.been.calledOnce;
        expect(studentExamsComponent.studentExams.length).to.equal(2);
        tick();
    }));

    it('should correctly catch HTTPError and get additional error when generating student exams', fakeAsync(() => {
        const translationService = TestBed.inject(TranslateService);
        const errorDetailString = 'artemisApp.exam.validation.tooFewExerciseGroups';
        const httpError = new HttpErrorResponse({
            error: { errorKey: errorDetailString },
            status: 400,
        });
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [], status: 200 })));
        generateStudentExamsStub.returns(throwError(httpError));

        studentExamsComponentFixture.detectChanges();
        studentExamsComponent.ngOnInit();

        expect(studentExamsComponent.studentExams.length).to.equal(0);
        const translationServiceSpy = sinon.spy(translationService, 'instant');
        const generateStudentExamsButton = studentExamsComponentFixture.debugElement.query(By.css('#generateStudentExamsButton'));
        expect(generateStudentExamsButton).to.exist;
        expect(generateStudentExamsButton.nativeElement.disabled).to.equal(false);
        generateStudentExamsButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        expect(translationServiceSpy).to.have.been.calledOnceWithExactly(errorDetailString);
        tick();
    }));

    it('should generate student exams after warning the user that the existing are deleted', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        studentExamsComponentFixture.detectChanges();

        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamStarted).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;
        const generateStudentExamsButton = studentExamsComponentFixture.debugElement.query(By.css('#generateStudentExamsButton'));
        expect(generateStudentExamsButton).to.exist;
        expect(generateStudentExamsButton.nativeElement.disabled).to.equal(false);
        expect(studentExamsComponent.studentExams.length).to.equal(2);
        generateStudentExamsButton.nativeElement.click();
        expect(modalServiceOpenStub).to.have.been.called;
        expect(generateStudentExamsStub).to.have.been.calledOnce;
        expect(studentExamsComponent.studentExams.length).to.equal(2);
        tick();
    }));

    it('should generate missing student exams', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne], status: 200 })));

        studentExamsComponentFixture.detectChanges();
        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne, studentExamTwo], status: 200 })));

        expect(studentExamsComponent.hasStudentsWithoutExam).to.equal(true);
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamStarted).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(studentExamsComponent.studentExams.length).to.equal(1);
        expect(course).to.exist;
        const generateStudentExamsSpy = sinon.spy(examManagementService, 'generateMissingStudentExams');
        const generateMissingStudentExamsButton = studentExamsComponentFixture.debugElement.query(By.css('#generateMissingStudentExamsButton'));
        expect(generateMissingStudentExamsButton).to.exist;
        expect(generateMissingStudentExamsButton.nativeElement.disabled).to.equal(false);
        expect(!!studentExamsComponent.studentExams && !!studentExamsComponent.studentExams.length).to.equal(true);
        generateMissingStudentExamsButton.nativeElement.click();
        expect(generateStudentExamsSpy).to.have.been.calledOnce;
        expect(studentExamsComponent.studentExams.length).to.equal(2);
        tick();
    }));

    it('should correctly catch HTTPError when generating missing student exams', fakeAsync(() => {
        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        generateMissingStudentExamsStub.returns(throwError(httpError));
        findAllStudentExamsStub.returns(of(new HttpResponse<StudentExam[]>({ body: [studentExamOne], status: 200 })));
        studentExamsComponentFixture.detectChanges();

        expect(studentExamsComponent.hasStudentsWithoutExam).to.equal(true);
        const generateMissingStudentExamsButton = studentExamsComponentFixture.debugElement.query(By.css('#generateMissingStudentExamsButton'));
        expect(generateMissingStudentExamsButton).to.exist;
        expect(generateMissingStudentExamsButton.nativeElement.disabled).to.equal(false);
        generateMissingStudentExamsButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should start the exercises of students', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');
        studentExamsComponentFixture.detectChanges();

        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamStarted).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;

        const startExercisesSpy = sinon.spy(examManagementService, 'startExercises');
        const startExercisesButton = studentExamsComponentFixture.debugElement.query(By.css('#startExercisesButton'));
        expect(startExercisesButton).to.exist;
        expect(startExercisesButton.nativeElement.disabled).to.equal(false);

        startExercisesButton.nativeElement.click();
        expect(startExercisesSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should unlock all repositories of the students', fakeAsync(() => {
        course.isAtLeastInstructor = true;

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;
        const unlockAllRepositoriesButton = studentExamsComponentFixture.debugElement.query(By.css('#handleUnlockAllRepositoriesButton'));
        expect(unlockAllRepositoriesButton).to.exist;
        expect(unlockAllRepositoriesButton.nativeElement.disabled).to.equal(false);
        unlockAllRepositoriesButton.nativeElement.click();
        expect(modalServiceOpenStub).to.have.been.called;
        expect(unlockAllRepositoriesStub).to.have.been.calledOnce;
        tick();
    }));

    it('should lock all repositories of the students', fakeAsync(() => {
        course.isAtLeastInstructor = true;

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;
        const lockAllRepositoriesButton = studentExamsComponentFixture.debugElement.query(By.css('#lockAllRepositoriesButton'));
        expect(lockAllRepositoriesButton).to.exist;
        expect(lockAllRepositoriesButton.nativeElement.disabled).to.equal(false);
        lockAllRepositoriesButton.nativeElement.click();
        expect(modalServiceOpenStub).to.have.been.called;
        expect(lockAllRepositoriesStub).to.have.been.calledOnce;
        tick();
    }));

    it('should correctly catch HTTPError when starting the exercises of the students', fakeAsync(() => {
        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        course.isAtLeastInstructor = true;
        exam.startDate = moment().add(120, 'seconds');

        startExercisesStub.returns(throwError(httpError));
        studentExamsComponentFixture.detectChanges();

        const startExercisesButton = studentExamsComponentFixture.debugElement.query(By.css('#startExercisesButton'));
        expect(startExercisesButton).to.exist;
        expect(startExercisesButton.nativeElement.disabled).to.equal(false);
        startExercisesButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should correctly catch HTTPError when unlocking all repositories', fakeAsync(() => {
        course.isAtLeastInstructor = true;

        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        unlockAllRepositoriesStub.returns(throwError(httpError));

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;

        const unlockAllRepositoriesButton = studentExamsComponentFixture.debugElement.query(By.css('#handleUnlockAllRepositoriesButton'));
        expect(unlockAllRepositoriesButton).to.exist;
        expect(unlockAllRepositoriesButton.nativeElement.disabled).to.equal(false);
        unlockAllRepositoriesButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should correctly catch HTTPError when locking all repositories', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        lockAllRepositoriesStub.returns(throwError(httpError));

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;

        const lockAllRepositoriesButton = studentExamsComponentFixture.debugElement.query(By.css('#lockAllRepositoriesButton'));
        expect(lockAllRepositoriesButton).to.exist;
        expect(lockAllRepositoriesButton.nativeElement.disabled).to.equal(false);
        lockAllRepositoriesButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));

    it('should evaluate Quiz exercises', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().subtract(200, 'seconds');
        exam.endDate = moment().subtract(100, 'seconds');

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamOver).to.equal(true);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;
        const evaluateQuizExercisesButton = studentExamsComponentFixture.debugElement.query(By.css('#evaluateQuizExercisesButton'));

        expect(evaluateQuizExercisesButton).to.exist;
        expect(evaluateQuizExercisesButton.nativeElement.disabled).to.equal(false);
        evaluateQuizExercisesButton.nativeElement.click();
        expect(evaluateQuizExercisesStub).to.have.been.calledOnce;
        tick();
    }));

    it('should correctly catch HTTPError when evaluating quiz exercises', fakeAsync(() => {
        course.isAtLeastInstructor = true;
        exam.startDate = moment().subtract(200, 'seconds');
        exam.endDate = moment().subtract(100, 'seconds');

        studentExamsComponentFixture.detectChanges();
        expect(studentExamsComponent.isLoading).to.equal(false);
        expect(studentExamsComponent.isExamOver).to.equal(true);
        expect(studentExamsComponent.course.isAtLeastInstructor).to.equal(true);
        expect(course).to.exist;

        const httpError = new HttpErrorResponse({ error: 'Forbidden', status: 403 });
        evaluateQuizExercisesStub.returns(throwError(httpError));
        studentExamsComponentFixture.detectChanges();

        const evaluateQuizExercisesButton = studentExamsComponentFixture.debugElement.query(By.css('#evaluateQuizExercisesButton'));
        expect(evaluateQuizExercisesButton).to.exist;
        expect(evaluateQuizExercisesButton.nativeElement.disabled).to.equal(false);
        evaluateQuizExercisesButton.nativeElement.click();
        expect(alertServiceSpy).to.have.been.calledOnce;
        tick();
    }));
});
