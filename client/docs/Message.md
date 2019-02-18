
# Message

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**appid** | **Integer** | The application id that send this message. | 
**date** | [**OffsetDateTime**](OffsetDateTime.md) | The date the message was created. | 
**extras** | **Map&lt;String, Object&gt;** | The extra data sent along the message.  The extra fields are stored in a key-value scheme. Only accepted in CreateMessage requests with application/json content-type.  The keys should be in the following format: &amp;lt;top-namespace&amp;gt;::[&amp;lt;sub-namespace&amp;gt;::]&amp;lt;action&amp;gt;  These namespaces are reserved and might be used in the official clients: gotify android ios web server client. Do not use them for other purposes. |  [optional]
**id** | **Integer** | The message id. | 
**message** | **String** | The actual message. | 
**priority** | **Long** | The priority of the message. |  [optional]
**title** | **String** | The title of the message. |  [optional]



