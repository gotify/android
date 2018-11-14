# TokenApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createApp**](TokenApi.md#createApp) | **POST** application | Create an application.
[**createClient**](TokenApi.md#createClient) | **POST** client | Create a client.
[**deleteApp**](TokenApi.md#deleteApp) | **DELETE** application/{id} | Delete an application.
[**deleteClient**](TokenApi.md#deleteClient) | **DELETE** client/{id} | Delete a client.
[**getApps**](TokenApi.md#getApps) | **GET** application | Return all applications.
[**getClients**](TokenApi.md#getClients) | **GET** client | Return all clients.
[**uploadAppImage**](TokenApi.md#uploadAppImage) | **POST** application/{id}/image | 


<a name="createApp"></a>
# **createApp**
> Application createApp(body)

Create an application.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
Application body = new Application(); // Application | the application to add
try {
    Application result = apiInstance.createApp(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#createApp");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Application**](Application.md)| the application to add |

### Return type

[**Application**](Application.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="createClient"></a>
# **createClient**
> Client createClient(body)

Create a client.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
Client body = new Client(); // Client | the client to add
try {
    Client result = apiInstance.createClient(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#createClient");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**Client**](Client.md)| the client to add |

### Return type

[**Client**](Client.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteApp"></a>
# **deleteApp**
> Void deleteApp(id)

Delete an application.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
Integer id = 56; // Integer | the application id
try {
    Void result = apiInstance.deleteApp(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#deleteApp");
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

<a name="deleteClient"></a>
# **deleteClient**
> Void deleteClient(id)

Delete a client.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
Integer id = 56; // Integer | the client id
try {
    Void result = apiInstance.deleteClient(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#deleteClient");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the client id |

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getApps"></a>
# **getApps**
> List&lt;Application&gt; getApps()

Return all applications.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
try {
    List<Application> result = apiInstance.getApps();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#getApps");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;Application&gt;**](Application.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getClients"></a>
# **getClients**
> List&lt;Client&gt; getClients()

Return all clients.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
try {
    List<Client> result = apiInstance.getClients();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#getClients");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;Client&gt;**](Client.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="uploadAppImage"></a>
# **uploadAppImage**
> Application uploadAppImage(file, id)



Upload an image for an application

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.TokenApi;

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

TokenApi apiInstance = new TokenApi();
File file = new File("/path/to/file.txt"); // File | the application image
Integer id = 56; // Integer | the application id
try {
    Application result = apiInstance.uploadAppImage(file, id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling TokenApi#uploadAppImage");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **File**| the application image |
 **id** | **Integer**| the application id |

### Return type

[**Application**](Application.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

