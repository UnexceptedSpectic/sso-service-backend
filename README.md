# Spring single sign-on (SSO) service

## Components

- This repository constitutes the API of this service.
- Access the frontend web app [here](http://auth.libredelibre.com/) and its sourcecode [here](https://github.com/UnexceptedSpectic/sso-frontend).
- Try a web app implementing this service [here](http://budget.libredelibre.com/).
- Confirm that [another web app](http://demo.libredelibre.com/) that is part of the same SSO suite as the one listed above responds to the sign in state of the suite. *Note that the webapps are hosted on different boxes.

## Implementation guidelines

1. Sign in, sign out, and jwt renewal operations must be completed via the web interface.
2. Only jwt verification can be done away from the SSO webapp.
3. Apps need to check to ensure their jwt isn't expired. When it does, they should redirect to the SSO web app to sign in.
4. Accessing the SSO web app on a signed in state will return all of the unexpired jwts representing logged in states for all users of a particular SSO suite. It is up to the developer to determine which jwt/account should be used with the app.

### SSO web app endpoints

#### Login

Path: `/login`

Query params:
- `ssoSuiteId`
- `redirectUrl`

#### Jwt renewal

Path: `/login`

Query params:
- `redirectUrl`
- `jwt`

Query params:
- `ssoSuiteId`
- `redirectUrl`

#### Logout

Path: `/logout`

Query params:
- `redirectUrl`
- `jwt`