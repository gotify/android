package com.github.gotify.client.api;

import com.github.gotify.client.CollectionFormats.*;

import retrofit2.Call;
import retrofit2.http.*;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import com.github.gotify.client.model.Health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
