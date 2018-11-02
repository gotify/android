# swagger-java-client

## Requirements

Building the API client library requires [Maven](https://maven.apache.org/) to be installed.

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn deploy
```

Refer to the [official documentation](https://maven.apache.org/plugins/maven-deploy-plugin/usage.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
    <groupId>io.swagger</groupId>
    <artifactId>swagger-java-client</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "io.swagger:swagger-java-client:1.0.0"
```

### Others

At first generate the JAR by executing:

    mvn package

Then manually install the following JARs:

* target/swagger-java-client-1.0.0.jar
* target/lib/*.jar

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

import com.github.gotify.client.*;
import com.github.gotify.client.auth.*;
import com.github.gotify.client.model.*;
import com.github.gotify.client.api.MessageApi;

import java.io.File;
import java.util.*;

public class MessageApiExample {

    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        
        // Configure API key authorization: appTokenHeader
        ApiKeyAuth appTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("appTokenHeader");
        appTokenHeader.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //appTokenHeader.setApiKeyPrefix("Token");

        // Configure API key authorization: appTokenQuery
        ApiKeyAuth appTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("appTokenQuery");
        appTokenQuery.setApiKey("YOUR API KEY");
        // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
        //appTokenQuery.setApiKeyPrefix("Token");

        MessageApi apiInstance = new MessageApi();
        Message body = new Message(); // Message | the message to add
        try {
            Message result = apiInstance.createMessage(body);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MessageApi#createMessage");
            e.printStackTrace();
        }
    }
}

```

## Documentation for API Endpoints

All URIs are relative to *http://localhost*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*MessageApi* | [**createMessage**](docs/MessageApi.md#createMessage) | **POST** /message | Create a message.
*MessageApi* | [**deleteAppMessages**](docs/MessageApi.md#deleteAppMessages) | **DELETE** /application/{id}/message | Delete all messages from a specific application.
*MessageApi* | [**deleteMessage**](docs/MessageApi.md#deleteMessage) | **DELETE** /message/{id} | Deletes a message with an id.
*MessageApi* | [**deleteMessages**](docs/MessageApi.md#deleteMessages) | **DELETE** /message | Delete all messages.
*MessageApi* | [**getAppMessages**](docs/MessageApi.md#getAppMessages) | **GET** /application/{id}/message | Return all messages from a specific application.
*MessageApi* | [**getMessages**](docs/MessageApi.md#getMessages) | **GET** /message | Return all messages.
*MessageApi* | [**streamMessages**](docs/MessageApi.md#streamMessages) | **GET** /stream | Websocket, return newly created messages.
*TokenApi* | [**createApp**](docs/TokenApi.md#createApp) | **POST** /application | Create an application.
*TokenApi* | [**createClient**](docs/TokenApi.md#createClient) | **POST** /client | Create a client.
*TokenApi* | [**deleteApp**](docs/TokenApi.md#deleteApp) | **DELETE** /application/{id} | Delete an application.
*TokenApi* | [**deleteClient**](docs/TokenApi.md#deleteClient) | **DELETE** /client/{id} | Delete a client.
*TokenApi* | [**getApps**](docs/TokenApi.md#getApps) | **GET** /application | Return all applications.
*TokenApi* | [**getClients**](docs/TokenApi.md#getClients) | **GET** /client | Return all clients.
*TokenApi* | [**uploadAppImage**](docs/TokenApi.md#uploadAppImage) | **POST** /application/{id}/image | 
*UserApi* | [**createUser**](docs/UserApi.md#createUser) | **POST** /user | Create a user.
*UserApi* | [**currentUser**](docs/UserApi.md#currentUser) | **GET** /current/user | Return the current user.
*UserApi* | [**deleteUser**](docs/UserApi.md#deleteUser) | **DELETE** /user/{id} | Deletes a user.
*UserApi* | [**getUser**](docs/UserApi.md#getUser) | **GET** /user/{id} | Get a user.
*UserApi* | [**getUsers**](docs/UserApi.md#getUsers) | **GET** /user | Return all users.
*UserApi* | [**updateCurrentUser**](docs/UserApi.md#updateCurrentUser) | **POST** /current/user/password | Update the password of the current user.
*UserApi* | [**updateUser**](docs/UserApi.md#updateUser) | **POST** /user/{id} | Update a user.
*VersionApi* | [**getVersion**](docs/VersionApi.md#getVersion) | **GET** /version | Get version information.


## Documentation for Models

 - [Application](docs/Application.md)
 - [Client](docs/Client.md)
 - [Error](docs/Error.md)
 - [Message](docs/Message.md)
 - [PagedMessages](docs/PagedMessages.md)
 - [Paging](docs/Paging.md)
 - [User](docs/User.md)
 - [UserPass](docs/UserPass.md)
 - [UserWithPass](docs/UserWithPass.md)
 - [VersionInfo](docs/VersionInfo.md)


## Documentation for Authorization

Authentication schemes defined for the API:
### appTokenHeader

- **Type**: API key
- **API key parameter name**: X-Gotify-Key
- **Location**: HTTP header

### appTokenQuery

- **Type**: API key
- **API key parameter name**: token
- **Location**: URL query string

### basicAuth

- **Type**: HTTP basic authentication

### clientTokenHeader

- **Type**: API key
- **API key parameter name**: X-Gotify-Key
- **Location**: HTTP header

### clientTokenQuery

- **Type**: API key
- **API key parameter name**: token
- **Location**: URL query string


## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author



