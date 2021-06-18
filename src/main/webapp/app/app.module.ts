import { ErrorHandler, LOCALE_ID, NgModule } from '@angular/core';
import { DatePipe, registerLocaleData } from '@angular/common';
import { HTTP_INTERCEPTORS, HttpClient, HttpClientModule } from '@angular/common/http';
import { Title } from '@angular/platform-browser';
import { JhiLanguageService, NgJhipsterModule } from 'ng-jhipster';
import { NgbDateAdapter, NgbDatepickerConfig } from '@ng-bootstrap/ng-bootstrap';
import * as moment from 'moment';
import { NgxWebstorageModule, SessionStorageService } from 'ngx-webstorage';
import { DifferencePipe, MomentModule } from 'ngx-moment';
import { FaIconLibrary } from '@fortawesome/angular-fontawesome';
import { fas } from '@fortawesome/free-solid-svg-icons';
import locale from '@angular/common/locales/en';
import { MissingTranslationHandler, TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { CookieService } from 'ngx-cookie-service';

import { BrowserModule } from '@angular/platform-browser';
import { ServiceWorkerModule } from '@angular/service-worker';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ArtemisSystemNotificationModule } from 'app/shared/notification/system-notification/system-notification.module';
import { NavbarComponent } from 'app/shared/layouts/navbar/navbar.component';
import { NotificationSidebarComponent } from 'app/shared/notification/notification-sidebar/notification-sidebar.component';
import { PageRibbonComponent } from 'app/shared/layouts/profiles/page-ribbon.component';
import { ArtemisHeaderExercisePageWithDetailsModule } from 'app/exercises/shared/exercise-headers/exercise-headers.module';
import { SystemNotificationComponent } from 'app/shared/notification/system-notification/system-notification.component';
import { ArtemisAppRoutingModule } from 'app/app-routing.module';
import { MainComponent } from 'app/shared/layouts/main/main.component';
import { SharedModule } from 'app/shared/shared.module';
import { ArtemisCoursesModule } from 'app/overview/courses.module';
import { ArtemisConnectionNotificationModule } from 'app/shared/notification/connection-notification/connection-notification.module';
import { FooterComponent } from 'app/shared/layouts/footer/footer.component';
import { ArtemisLegalModule } from 'app/core/legal/legal.module';
import { ActiveMenuDirective } from 'app/shared/layouts/navbar/active-menu.directive';
import { ErrorComponent } from 'app/shared/layouts/error/error.component';
import { GuidedTourModule } from 'app/guided-tour/guided-tour.module';
import { ArtemisComplaintsModule } from 'app/complaints/complaints.module';
import { ArtemisHomeModule } from 'app/home/home.module';
import { OrionOutdatedComponent } from 'app/shared/orion/outdated-plugin-warning/orion-outdated.component';
import { ArtemisTeamModule } from 'app/exercises/shared/team/team.module';
import { LoadingNotificationComponent } from 'app/shared/notification/loading-notification/loading-notification.component';
import { NotificationPopupComponent } from 'app/shared/notification/notification-popup/notification-popup.component';
import { missingTranslationHandler, translatePartialLoader } from './shared/constants/translation.config';
import { NgbDateMomentAdapter } from './shared/util/datepicker-adapter';
import { SentryErrorHandler } from './core/sentry/sentry.error-handler';
import { AuthInterceptor } from './core/interceptor/auth.interceptor';
import { AuthExpiredInterceptor } from './core/interceptor/auth-expired.interceptor';
import { ErrorHandlerInterceptor } from './core/interceptor/errorhandler.interceptor';
import { BrowserFingerprintInterceptor } from './core/interceptor/browser-fingerprint.interceptor.service';
import { NotificationInterceptor } from './core/interceptor/notification.interceptor';
import { RepositoryInterceptor } from './exercises/shared/result/repository.service';
import { LoadingNotificationInterceptor } from './shared/notification/loading-notification/loading-notification.interceptor';
import { ArtemisVersionInterceptor } from './core/interceptor/artemis-version.interceptor';
import { fontAwesomeIcons } from './core/icons/font-awesome-icons';

// NOTE: this module should only include the most important modules for normal users, all course management, admin and account functionality should be lazy loaded if possible
@NgModule({
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        SharedModule,
        ArtemisHomeModule,
        ArtemisAppRoutingModule,
        // Set this to true to enable service worker (PWA)
        ServiceWorkerModule.register('ngsw-worker.js', { enabled: false }),
        ArtemisConnectionNotificationModule,
        GuidedTourModule,
        ArtemisLegalModule,
        ArtemisTeamModule,
        ArtemisCoursesModule,
        ArtemisSystemNotificationModule,
        ArtemisComplaintsModule,
        ArtemisHeaderExercisePageWithDetailsModule,
        HttpClientModule,
        NgxWebstorageModule.forRoot({ prefix: 'jhi', separator: '-' }),
        /**
         * @external MomentModule is a date library for parsing, validating, manipulating, and formatting dates.
         */
        MomentModule,
        NgJhipsterModule.forRoot({
            // set below to true to make alerts look like toast
            alertAsToast: false,
            alertTimeout: 8000,
            i18nEnabled: true,
            defaultI18nLang: 'en',
        }),
        TranslateModule.forRoot({
            loader: {
                provide: TranslateLoader,
                useFactory: translatePartialLoader,
                deps: [HttpClient],
            },
            missingTranslationHandler: {
                provide: MissingTranslationHandler,
                useFactory: missingTranslationHandler,
            },
        }),
    ],
    providers: [
        Title,
        { provide: LOCALE_ID, useValue: 'en' },
        { provide: NgbDateAdapter, useClass: NgbDateMomentAdapter },
        { provide: ErrorHandler, useClass: SentryErrorHandler },
        DatePipe,
        DifferencePipe,
        CookieService,
        /**
         * @description Interceptor declarations:
         * Interceptors are located at 'blocks/interceptor/.
         * All of them implement the HttpInterceptor interface.
         * They can be used to modify API calls or trigger additional function calls.
         * Most interceptors will transform the outgoing request before passing it to
         * the next interceptor in the chain, by calling next.handle(transformedReq).
         * Documentation: https://angular.io/api/common/http/HttpInterceptor
         */
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthExpiredInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: ErrorHandlerInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: BrowserFingerprintInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: NotificationInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: RepositoryInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: LoadingNotificationInterceptor,
            multi: true,
        },
        {
            provide: HTTP_INTERCEPTORS,
            useClass: ArtemisVersionInterceptor,
            multi: true,
        },
    ],
    declarations: [
        MainComponent,
        NavbarComponent,
        ErrorComponent,
        OrionOutdatedComponent,
        PageRibbonComponent,
        ActiveMenuDirective,
        FooterComponent,
        NotificationPopupComponent,
        NotificationSidebarComponent,
        SystemNotificationComponent,
        LoadingNotificationComponent,
    ],
    bootstrap: [MainComponent],
})
export class ArtemisAppModule {
    constructor(
        iconLibrary: FaIconLibrary,
        dpConfig: NgbDatepickerConfig,
        languageService: JhiLanguageService,
        translateService: TranslateService,
        sessionStorageService: SessionStorageService,
    ) {
        registerLocaleData(locale);
        iconLibrary.addIconPacks(fas);
        iconLibrary.addIcons(...fontAwesomeIcons);
        dpConfig.minDate = { year: moment().year() - 100, month: 1, day: 1 };
        languageService.init();
        translateService.setDefaultLang('en');
        // if user have changed language and navigates away from the application and back to the application then use previously choosed language
        const langKey = sessionStorageService.retrieve('locale') ?? 'en';
        translateService.use(langKey);
    }
}
