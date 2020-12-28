# HealthApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getHealth**](HealthApi.md#getHealth) | **GET** health | Get health information.


<a name="getHealth"></a>
# **getHealth**
> Health getHealth()

Get health information.

### Example
```java
// Import classes:
//import com.github.gotify.client.ApiException;
//import com.github.gotify.client.api.HealthApi;


HealthApi apiInstance = new HealthApi();
try {
    Health result = apiInstance.getHealth();
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling HealthApi#getHealth");
    e.printStackTrace();
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Health**](Health.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

