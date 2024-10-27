package com.github.gotify.client.api;

import com.github.gotify.client.CollectionFormats.*;

import retrofit2.Call;
import retrofit2.http.*;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import com.github.gotify.client.model.CreateUserExternal;
import com.github.gotify.client.model.Error;
import com.github.gotify.client.model.UpdateUserExternal;
import com.github.gotify.client.model.User;
import com.github.gotify.client.model.UserPass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UserApi {
  /**
   * Create a user.
   * With enabled registration: non admin users can be created without authentication. With disabled registrations: users can only be created by admin users.
   * @param body the user to add (required)
   * @return Call&lt;User&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("user")
  Call<User> createUser(
    @retrofit2.http.Body CreateUserExternal body
  );

  /**
   * Return the current user.
   * 
   * @return Call&lt;User&gt;
   */
  @GET("current/user")
  Call<User> currentUser();
    

  /**
   * Deletes a user.
   * 
   * @param id the user id (required)
   * @return Call&lt;Void&gt;
   */
  @DELETE("user/{id}")
  Call<Void> deleteUser(
    @retrofit2.http.Path("id") Long id
  );

  /**
   * Get a user.
   * 
   * @param id the user id (required)
   * @return Call&lt;User&gt;
   */
  @GET("user/{id}")
  Call<User> getUser(
    @retrofit2.http.Path("id") Long id
  );

  /**
   * Return all users.
   * 
   * @return Call&lt;List&lt;User&gt;&gt;
   */
  @GET("user")
  Call<List<User>> getUsers();
    

  /**
   * Update the password of the current user.
   * 
   * @param body the user (required)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("current/user/password")
  Call<Void> updateCurrentUser(
    @retrofit2.http.Body UserPass body
  );

  /**
   * Update a user.
   * 
   * @param body the updated user (required)
   * @param id the user id (required)
   * @return Call&lt;User&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("user/{id}")
  Call<User> updateUser(
    @retrofit2.http.Body UpdateUserExternal body, @retrofit2.http.Path("id") Long id
  );

}
