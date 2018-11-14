# VersionApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getVersion**](VersionApi.md#getVersion) | **GET** version | Get version information.


<a name="getVersion"></a>
# **getVersion**
> VersionInfo getVersion()

Get version information.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.api.VersionApi;


VersionApi apiInstance = new VersionApi();
try {
    VersionInfo result = apiInstance.getVersion();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling VersionApi#getVersion");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**VersionInfo**](VersionInfo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

