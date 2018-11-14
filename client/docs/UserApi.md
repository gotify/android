# UserApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createUser**](UserApi.md#createUser) | **POST** user | Create a user.
[**currentUser**](UserApi.md#currentUser) | **GET** current/user | Return the current user.
[**deleteUser**](UserApi.md#deleteUser) | **DELETE** user/{id} | Deletes a user.
[**getUser**](UserApi.md#getUser) | **GET** user/{id} | Get a user.
[**getUsers**](UserApi.md#getUsers) | **GET** user | Return all users.
[**updateCurrentUser**](UserApi.md#updateCurrentUser) | **POST** current/user/password | Update the password of the current user.
[**updateUser**](UserApi.md#updateUser) | **POST** user/{id} | Update a user.


<a name="createUser"></a>
# **createUser**
> User createUser(body)

Create a user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
UserWithPass body = new UserWithPass(); // UserWithPass | the user to add
try {
    User result = apiInstance.createUser(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#createUser");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UserWithPass**](UserWithPass.md)| the user to add |

### Return type

[**User**](User.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="currentUser"></a>
# **currentUser**
> User currentUser()

Return the current user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
try {
    User result = apiInstance.currentUser();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#currentUser");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**User**](User.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="deleteUser"></a>
# **deleteUser**
> Void deleteUser(id)

Deletes a user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
Integer id = 56; // Integer | the user id
try {
    Void result = apiInstance.deleteUser(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#deleteUser");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the user id |

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getUser"></a>
# **getUser**
> User getUser(id)

Get a user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
Integer id = 56; // Integer | the user id
try {
    User result = apiInstance.getUser(id);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#getUser");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the user id |

### Return type

[**User**](User.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="getUsers"></a>
# **getUsers**
> List&lt;User&gt; getUsers()

Return all users.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
try {
    List<User> result = apiInstance.getUsers();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#getUsers");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List&lt;User&gt;**](User.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="updateCurrentUser"></a>
# **updateCurrentUser**
> Void updateCurrentUser(body)

Update the password of the current user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
UserPass body = new UserPass(); // UserPass | the user
try {
    Void result = apiInstance.updateCurrentUser(body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#updateCurrentUser");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**UserPass**](UserPass.md)| the user |

### Return type

[**Void**](.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

<a name="updateUser"></a>
# **updateUser**
> User updateUser(id, body)

Update a user.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiClient;
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.Configuration;
//import com.github.gotify.client.auth.*;
//import com.github.gotify.client.api.UserApi;

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

UserApi apiInstance = new UserApi();
Integer id = 56; // Integer | the user id
UserWithPass body = new UserWithPass(); // UserWithPass | the updated user
try {
    User result = apiInstance.updateUser(id, body);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling UserApi#updateUser");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Integer**| the user id |
 **body** | [**UserWithPass**](UserWithPass.md)| the updated user |

### Return type

[**User**](User.md)

### Authorization

[basicAuth](../README.md#basicAuth), [clientTokenHeader](../README.md#clientTokenHeader), [clientTokenQuery](../README.md#clientTokenQuery)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

