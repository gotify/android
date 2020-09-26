package com.github.alertify.client.api;

import retrofit2.Call;
import retrofit2.http.*;

import com.github.alertify.client.model.VersionInfo;

public interface VersionApi {
  /**
   * Get version information.
   * 
   * @return Call&lt;VersionInfo&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @GET("version")
  Call<VersionInfo> getVersion();
    

}
