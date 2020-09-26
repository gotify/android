package com.github.alertify.client.api;

import retrofit2.Call;
import retrofit2.http.*;

import com.github.alertify.client.model.Health;

public interface HealthApi {
  /**
   * Get health information.
   * 
   * @return Call&lt;Health&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("health")
  Call<Health> getHealth();
    

}
