# MessageApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createMessage**](MessageApi.md#createMessage) | **POST** message | Create a message.
[**deleteAppMessages**](MessageApi.md#deleteAppMessages) | **DELETE** application/{id}/message | Delete all messages from a specific application.
[**deleteMessage**](MessageApi.md#deleteMessage) | **DELETE** message/{id} | Deletes a message with an id.
[**deleteMessages**](MessageApi.md#deleteMessages) | **DELETE** message | Delete all messages.
[**getAppMessages**](MessageApi.md#getAppMessages) | **GET** application/{id}/message | Return all messages from a specific application.
[**getMessages**](MessageApi.md#getMessages) | **GET** message | Return all messages.
[**streamMessages**](MessageApi.md#streamMessages) | **GET** stream | Websocket, return newly created messages.


<a name="createMessage"></a>
# **createMessage**
> Message createMessage(body)

Create a message.

__NOTE__: This API ONLY accepts an application token as authentication.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

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
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Message**](Message.md)| the message to add |

### Return type

[**Message**](Message.md)

### Authorization

[appTokenHeader](../README.md#appTokenHeader), [appTokenQuery](../README.md#appTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteAppMessages"></a>
# **deleteAppMessages**
> Void deleteAppMessages(id)

Delete all messages from a specific application.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
Integer id = 56; // Integer | the application id
try {
    Void result = apiInstance.deleteAppMessages(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#deleteAppMessages");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the application id |

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteMessage"></a>
# **deleteMessage**
> Void deleteMessage(id)

Deletes a message with an id.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
Integer id = 56; // Integer | the message id
try {
    Void result = apiInstance.deleteMessage(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#deleteMessage");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the message id |

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteMessages"></a>
# **deleteMessages**
> Void deleteMessages()

Delete all messages.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
try {
    Void result = apiInstance.deleteMessages();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#deleteMessages");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getAppMessages"></a>
# **getAppMessages**
> PagedMessages getAppMessages(id, limit, since)

Return all messages from a specific application.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
Integer id = 56; // Integer | the application id
Integer limit = 100; // Integer | the maximal amount of messages to return
Integer since = 56; // Integer | return all messages with an ID less than this value
try {
    PagedMessages result = apiInstance.getAppMessages(id, limit, since);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#getAppMessages");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the application id |
 **limit** | **Integer**| the maximal amount of messages to return | [optional] [default to 100]
 **since** | **Integer**| return all messages with an ID less than this value | [optional]

### Return type

[**PagedMessages**](PagedMessages.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getMessages"></a>
# **getMessages**
> PagedMessages getMessages(limit, since)

Return all messages.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
Integer limit = 100; // Integer | the maximal amount of messages to return
Integer since = 56; // Integer | return all messages with an ID less than this value
try {
    PagedMessages result = apiInstance.getMessages(limit, since);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#getMessages");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **limit** | **Integer**| the maximal amount of messages to return | [optional] [default to 100]
 **since** | **Integer**| return all messages with an ID less than this value | [optional]

### Return type

[**PagedMessages**](PagedMessages.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="streamMessages"></a>
# **streamMessages**
> Message streamMessages()

Websocket, return newly created messages.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.MessageApi;

ApiClient defaultClient = Configuration.getDefaultApiClient();

// Configure HTTP basic authorization: basicAuth
HttpBasicAuth basicAuth = (HttpBasicAuth) defaultClient.getAuthentication("basicAuth");
basicAuth.setUsername("YOUR USERNAME");
basicAuth.setPassword("YOUR PASSWORD");

// Configure API key authorization: clientTokenHeader
ApiKeyAuth clientTokenHeader = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenHeader");
clientTokenHeader.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenHeader.setApiKeyPrefix("Token");

// Configure API key authorization: clientTokenQuery
ApiKeyAuth clientTokenQuery = (ApiKeyAuth) defaultClient.getAuthentication("clientTokenQuery");
clientTokenQuery.setApiKey("YOUR API KEY");
// Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
//clientTokenQuery.setApiKeyPrefix("Token");

MessageApi apiInstance = new MessageApi();
try {
    Message result = apiInstance.streamMessages();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MessageApi#streamMessages");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Message**](Message.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

