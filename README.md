# Spring single sign-on (SSO) service

## Architecture

![architecture-diagram](https://github.com/UnexceptedSpectic/sso-service-backend/blob/main/arch.svg?raw=true "Architecture Diagram")

## Usage

It is recommended that developers use the front end UI associated with this service available [here](https://github.com/UnexceptedSpectic/sso-frontend). In such a case, the developer need only set up mongoDB, register for a developer account, and create a SSO suite.

### MongoDB
Install mongodb and configure user permissions e.g.  
`use admin;`  
`db.createUser(
{
user: "admin",
pwd: passwordPrompt(),
roles: [ { role: "userAdminAnyDatabase", db: "admin" }, "readWriteAnyDatabase" ]
}
);`

`use sso;`  
`db.createUser(
{
user: "sso",
pwd: passwordPrompt(),
roles: [ { role: "readWrite", db: "sso" } ]
}
);`

Enable authorization in the mongo configuration file and restart the mongod service. See more detailed instructions [here](https://docs.mongodb.com/manual/tutorial/enable-authentication/).  

Modify the `resources/application.properties.template` config file and rename it to `application.properties`.

### API Endpoints
If the SSO web app is used, developers should only utilize the `/account/authenticate`, of those listed below.

#### Register an account
Endpoint:  
`/account/create`

Sample reqeust body:  
`{
"username": "user1",
"email": "user1@email.com",
"password": "A)1ansmfdlasdf"
}`

- A password must have at least 8 characters, one capital letter, one symbol, and one number.
- Developers should specify that their account is of that type by adding the below:  
`type: developer`. Note the `apiKey` returned in the response body. An existing account can be changed to developer by using the `/account/changeType` endpoint.

#### Modify account type

Endpoint:  
`/account/changeType`

Sample request body:  
`{
"username": "user1",
"password": "A)1ansmfdlasdf",
"type": "developer"
}`

Types:   
- `"user"` (default)
- `"developer"`  
 

 Accounts changed to `"developer"` are provided with an `apiKey` in a successful response.

#### Register a SSO suite

Endpoint:  
`/sso-suite/create`

Sample request body:  
`{
"username": "user1",
"password": "A)1ansmfdlasdf",
"apiKey": "60417dbf2e99d717018b69c2",
"ssoSuiteName": "google"
}`

- The account creating a SSO suite must be of type `"developer"`; an `apiKey` is required.
- Save the returned `ssoSuiteId`.

#### Sign in/authenticate an account

This endpoint can be used to sign users into a SSO suite or to validate that a user is still signed in to one. In the first case, a new JWT is generated; in the second, if the JWT has not expired, it is returned. A user may be simultaneously signed into multiple SSO suites, the service providing and storing a JWT for each.

Endpoint:  
`/account/authenticate`  

Sample request body:  
`{
"username": "user1",
"password": "A)1ansmfdlasdf",
"ssoSuiteId": "604186342e99d717018b69c4"
}`

- Users are authenticated against a specific SSO suite.
- A successful response will return a `jwt` that can be used in place of username/email and password to authenticate in the future.

#### Sign out an account

Clear the JWT associated with a particular SSO suite, to sign an account out.

Endpoint:  
`/account/signOut`  

Sample request body:  
`{
"jwt": "eyJ0eXBlIjoiSldUIiwiYWxnIjoiSFM1MTIifQ.eyJzc29TdWl0ZUlkIjoiNjA0MTg2MzQyZTk5ZDcxNzAxOGI2OWM0IiwidXNlcklkIjoidGVzMjM0dCIsImV4cCI6MTYxNDkxNzcwMH0.ciaBuU2ImJlLS571oroif8CIeP5pOeDz-6746rHvkHZh1oTsbaLYn620AFsawOBGFFrIeS1JC28ksSzfAZ8lmw"
}`

### JWTs

#### Payload

Example:
`{
"ssoSuiteId": "604186342e99d717018b69c4",
"userId": "tes234t",
"exp": 1614917700
}`

Where `userId` may be either the `username` or `email` of an account.

## TODO
- Store user authorization scope in database and include it in JWT payloads.