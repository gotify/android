package com.github.gotify.client.api;

import com.github.gotify.client.CollectionFormats.*;

import retrofit2.Call;
import retrofit2.http.*;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import com.github.gotify.client.model.VersionInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
