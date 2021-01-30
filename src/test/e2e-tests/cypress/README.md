# Notes on using Cypress

### Using cypress
* First you need to start client and server of Artemis
* Then open the e2e-tests folder in your terminal
* You can start the cypress GUI with the command `` npx cypress open ``
  (yarn does not work, as it does not recognize the existing cypress folder
  -> it will create a new one in the Artemis root folder)
  
### License information
* The cypress Test runner (local setup) is free and open source
* Using cypress in CI with cypress dashboard usually costs money, but there is a free license for non-commercial, open source software 
  (see [here](https://www.cypress.io/oss-plan/))
