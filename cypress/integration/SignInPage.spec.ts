/// <reference types="cypress" />

describe('SignIn Page', () => {
    beforeEach(() => {
        cy.server();

        cy.route('POST', '**/authenticate').as('authenticate');
        cy.route('Get', '**/for-dashboard').as('dashboard');
    });

    it('should open SignIn page and log in successfully', () => {
        cy.visit('/');
        // @ts-ignore
        cy.get('#username').type(Cypress.config('username'));
        // @ts-ignore
        cy.get('#password').type(Cypress.config('password'));
        cy.get('.btn').click();
        cy.wait('@authenticate');
        cy.url().should('contain', 'courses');
        cy.wait('@dashboard');
        cy.get('.btn').should('contain.text', 'Sign up for a course');
    });
});
