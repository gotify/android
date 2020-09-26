package com.github.alertify.client.api;

import retrofit2.Call;
import retrofit2.http.*;

import com.github.alertify.client.model.Message;
import com.github.alertify.client.model.PagedMessages;

public interface MessageApi {
  /**
   * Create a message.
   * __NOTE__: This API ONLY accepts an application token as authentication.
   * @param body the message to add (required)
   * @return Call&lt;Message&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("message")
  Call<Message> createMessage(
    @retrofit2.http.Body Message body
  );

  /**
   * Delete all messages from a specific application.
   * 
   * @param id the application id (required)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @DELETE("application/{id}/message")
  Call<Void> deleteAppMessages(
    @retrofit2.http.Path("id") Long id
  );

  /**
   * Deletes a message with an id.
   * 
   * @param id the message id (required)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @DELETE("message/{id}")
  Call<Void> deleteMessage(
    @retrofit2.http.Path("id") Long id
  );

  /**
   * Delete all messages.
   * 
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @DELETE("message")
  Call<Void> deleteMessages();
    

  /**
   * Return all messages from a specific application.
   * 
   * @param id the application id (required)
   * @param limit the maximal amount of messages to return (optional, default to 100)
   * @param since return all messages with an ID less than this value (optional)
   * @return Call&lt;PagedMessages&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("application/{id}/message")
  Call<PagedMessages> getAppMessages(
    @retrofit2.http.Path("id") Long id, @retrofit2.http.Query("limit") Integer limit, @retrofit2.http.Query("since") Long since
  );

  /**
   * Return all messages.
   * 
   * @param limit the maximal amount of messages to return (optional, default to 100)
   * @param since return all messages with an ID less than this value (optional)
   * @return Call&lt;PagedMessages&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("message")
  Call<PagedMessages> getMessages(
    @retrofit2.http.Query("limit") Integer limit, @retrofit2.http.Query("since") Long since
  );

  /**
   * Websocket, return newly created messages.
   * 
   * @return Call&lt;Message&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("stream")
  Call<Message> streamMessages();
    

}
