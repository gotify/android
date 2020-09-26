package com.github.alertify.client.api;

import retrofit2.Call;
import retrofit2.http.*;

import com.github.alertify.client.model.Client;

import java.util.List;

public interface ClientApi {
  /**
   * Create a client.
   * 
   * @param body the client to add (required)
   * @return Call&lt;Client&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("client")
  Call<Client> createClient(
    @retrofit2.http.Body Client body
  );

  /**
   * Delete a client.
   * 
   * @param id the client id (required)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @DELETE("client/{id}")
  Call<Void> deleteClient(
    @retrofit2.http.Path("id") Long id
  );

  /**
   * Return all clients.
   * 
   * @return Call&lt;List&lt;Client&gt;&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("client")
  Call<List<Client>> getClients();
    

  /**
   * Update a client.
   * 
   * @param body the client to update (required)
   * @param id the client id (required)
   * @return Call&lt;Client&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @PUT("client/{id}")
  Call<Client> updateClient(
    @retrofit2.http.Body Client body, @retrofit2.http.Path("id") Long id
  );

}
