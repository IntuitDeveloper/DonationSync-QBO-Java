# Java Sample App For Tracking Pledges and Donations for NonProfits

The [Intuit Developer team](https://developer.intuit.com) has written this OAuth 2.0 Sample App in Java to provide working examples on how to track pledges and donations for a non-profit organization in QuickBooks.

## Table of Contents

* [Requirements](#requirements)
* [First Use Instructions](#first-use-instructions)
* [Running the code](#running-the-code)
* [Configuring the callback endpoint](#configuring-the-callback-endpoint)
* [High Level Flow](#high-level-flow)
* [Scope](#scope)
* [Storing the Tokens](#storing-the-tokens)
* [Discovery document](#discovery-document)


## Requirements

In order to successfully run this sample app you need a few things:

1. Java 1.8
2. A [developer.intuit.com](http://developer.intuit.com) account
3. An app on [developer.intuit.com](http://developer.intuit.com) and the associated client id and client secret.
 
## First Use Instructions

1. Clone the GitHub repo to your computer
2. Fill in the [`application.properties`](src/main/resources/application.properties) file values (OAuth2AppClientId, OAuth2AppClientSecret) by copying over from the keys section for your app.

## Running the code

Once the sample app code is on your computer, you can do the following steps to run the app:

1. cd to the project directory</li>
2. Run the command:`./gradlew bootRun` (Mac OS) or `gradlew.bat bootRun` (Windows)</li>
3. Wait until the terminal output displays the "Started Application in xxx seconds" message.
4. Your app should be up now in http://localhost:8080/ 
5. The oauth2 callback endpoint in the sample app is http://localhost:8080/oauth2redirect
6. To run the code on a different port, uncomment and update server.port property in application.properties. Also make sure to update the url in application.properties and in the Developer portal ("Keys" section).

## Configuring the callback endpoint
You'll have to set a Redirect URI in the Developer Portal ("Keys" section). With this app, the typical value would be http://localhost:8080/oauth2redirect, unless you host this sample app in a different way (if you were testing HTTPS, for example).

Note: Using localhost and http will only work when developing, using the sandbox credentials. Once you use production credentials, you'll need to host your app over https.

## High Level Flow

The sample app supports the following flows:

**Connect To QuickBooks** - This flow depicts the OAuth handshake to generate the access token for a specific company. It uses Accounting as the scope during the OAuth2 API call.

**Sync Customer** - This flow Queries the list of Customers (Donor's) from your QuickBooks Company.

**Make a Pledge** - This flow shows how to make a QuickBooks API call to create a pledge (invoice) for a donor.

**Make a Pledge** - This flow shows how to make a QuickBooks API call to create a donation (payment) for a donor.


## Scope

It is important to ensure that the scopes your are requesting match the scopes allowed on the Developer Portal.  For this sample app to work by default, your app on Developer Portal must support Accounting scopes.  If you'd like to support both Accounting and Payment, simply add the`com.intuit.quickbooks.payment` scope in the `application.properties` file.

## Storing the tokens
This app stores all the tokens and user information in the session. For production ready app, tokens should be encrypted and stored in a database.
