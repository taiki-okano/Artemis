import { Component, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { ActivatedRouteSnapshot, NavigationEnd, NavigationError, Router } from '@angular/router';
import { ProfileInfo } from 'app/shared/layouts/profiles/profile-info.model';
import { ProfileService } from 'app/shared/layouts/profiles/profile.service';
import { SentryErrorHandler } from 'app/core/sentry/sentry.error-handler';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import * as moment from 'moment';
import { Title } from '@angular/platform-browser';

@Component({
    selector: 'jhi-main',
    templateUrl: './main.component.html',
})
export class MainComponent implements OnInit {
    private renderer: Renderer2;

    constructor(
        private titleService: Title,
        private router: Router,
        private profileService: ProfileService,
        private sentryErrorHandler: SentryErrorHandler,
        private translateService: TranslateService,
        rootRenderer: RendererFactory2,
    ) {
        this.renderer = rootRenderer.createRenderer(document.querySelector('html'), null);
        this.setupErrorHandling().then(null);
    }

    private async setupErrorHandling() {
        this.profileService.getProfileInfo().subscribe((profileInfo: ProfileInfo) => {
            // sentry is only activated if it was specified in the application.yml file
            this.sentryErrorHandler.initSentry(profileInfo);
        });
    }

    ngOnInit() {
        this.router.events.subscribe((event) => {
            if (event instanceof NavigationEnd) {
                this.updateTitle();
            }
            if (event instanceof NavigationError && event.error.status === 404) {
                // noinspection JSIgnoredPromiseFromCall
                this.router.navigate(['/404']);
            }
        });

        this.translateService.onLangChange.subscribe((langChangeEvent: LangChangeEvent) => {
            this.updateTitle();
            moment.locale(langChangeEvent.lang);
            this.renderer.setAttribute(document.querySelector('html'), 'lang', langChangeEvent.lang);
        });
    }

    private getPageTitle(routeSnapshot: ActivatedRouteSnapshot): string {
        let title: string = routeSnapshot.data['pageTitle'] ?? '';
        if (routeSnapshot.firstChild) {
            title = this.getPageTitle(routeSnapshot.firstChild) || title;
        }
        return title;
    }

    private updateTitle(): void {
        let pageTitle = this.getPageTitle(this.router.routerState.snapshot.root);
        if (!pageTitle) {
            pageTitle = 'global.title';
        }
        this.translateService.get(pageTitle).subscribe((title) => this.titleService.setTitle(title));
    }
}
