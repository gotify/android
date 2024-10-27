# Application

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**defaultPriority** | **Long** | The default priority of messages sent by this application. Defaults to 0. |  [optional]
**description** | **String** | The description of the application. | 
**id** | **Long** | The application id. | 
**image** | **String** | The image of the application. | 
**internal** | **Boolean** | Whether the application is an internal application. Internal applications should not be deleted. | 
**lastUsed** | [**OffsetDateTime**](OffsetDateTime.md) | The last time the application token was used. |  [optional]
**name** | **String** | The application name. This is how the application should be displayed to the user. | 
**token** | **String** | The application token. Can be used as &#x60;appToken&#x60;. See Authentication. | 
