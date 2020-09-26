/*
 * Gotify REST-API.
 * This is the documentation of the Gotify REST-API.  # Authentication In Gotify there are two token types: __clientToken__: a client is something that receives message and manages stuff like creating new tokens or delete messages. (f.ex this token should be used for an android app) __appToken__: an application is something that sends messages (f.ex. this token should be used for a shell script)  The token can be either transmitted through a header named `X-Gotify-Key` or a query parameter named `token`. There is also the possibility to authenticate through basic auth, this should only be used for creating a clientToken.  \\---  Found a bug or have some questions? [Create an issue on GitHub](https://github.com/gotify/server/issues)
 *
 * OpenAPI spec version: 2.0.1
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.github.alertify.client.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * The Error contains error relevant information.
 */
@ApiModel(description = "The Error contains error relevant information.")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2020-06-24T18:39:01.386+02:00")
public class Error {
  @SerializedName("error")
  private String error = null;

  @SerializedName("errorCode")
  private Long errorCode = null;

  @SerializedName("errorDescription")
  private String errorDescription = null;

  public Error error(String error) {
    this.error = error;
    return this;
  }

   /**
   * The general error message
   * @return error
  **/
  @ApiModelProperty(example = "Unauthorized", required = true, value = "The general error message")
  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Error errorCode(Long errorCode) {
    this.errorCode = errorCode;
    return this;
  }

   /**
   * The http error code.
   * @return errorCode
  **/
  @ApiModelProperty(example = "401", required = true, value = "The http error code.")
  public Long getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(Long errorCode) {
    this.errorCode = errorCode;
  }

  public Error errorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
    return this;
  }

   /**
   * The http error code.
   * @return errorDescription
  **/
  @ApiModelProperty(example = "you need to provide a valid access token or user credentials to access this api", required = true, value = "The http error code.")
  public String getErrorDescription() {
    return errorDescription;
  }

  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return Objects.equals(this.error, error.error) &&
        Objects.equals(this.errorCode, error.errorCode) &&
        Objects.equals(this.errorDescription, error.errorDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(error, errorCode, errorDescription);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Error {\n");
    
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("    errorDescription: ").append(toIndentedString(errorDescription)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

