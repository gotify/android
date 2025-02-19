/*
 * Gotify REST-API.
 * This is the documentation of the Gotify REST-API.  # Authentication In Gotify there are two token types: __clientToken__: a client is something that receives message and manages stuff like creating new tokens or delete messages. (f.ex this token should be used for an android app) __appToken__: an application is something that sends messages (f.ex. this token should be used for a shell script)  The token can be transmitted in a header named `X-Gotify-Key`, in a query parameter named `token` or through a header named `Authorization` with the value prefixed with `Bearer` (Ex. `Bearer randomtoken`). There is also the possibility to authenticate through basic auth, this should only be used for creating a clientToken.  \\---  Found a bug or have some questions? [Create an issue on GitHub](https://github.com/gotify/server/issues)
 *
 * OpenAPI spec version: 2.0.2
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.github.gotify.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
/**
 * Used for updating a user.
 */
@Schema(description = "Used for updating a user.")


public class UpdateUserExternal {
  @SerializedName("admin")
  private Boolean admin = null;

  @SerializedName("name")
  private String name = null;

  @SerializedName("pass")
  private String pass = null;

  public UpdateUserExternal admin(Boolean admin) {
    this.admin = admin;
    return this;
  }

   /**
   * If the user is an administrator.
   * @return admin
  **/
  @Schema(example = "true", required = true, description = "If the user is an administrator.")
  public Boolean isAdmin() {
    return admin;
  }

  public void setAdmin(Boolean admin) {
    this.admin = admin;
  }

  public UpdateUserExternal name(String name) {
    this.name = name;
    return this;
  }

   /**
   * The user name. For login.
   * @return name
  **/
  @Schema(example = "unicorn", required = true, description = "The user name. For login.")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateUserExternal pass(String pass) {
    this.pass = pass;
    return this;
  }

   /**
   * The user password. For login. Empty for using old password
   * @return pass
  **/
  @Schema(example = "nrocinu", description = "The user password. For login. Empty for using old password")
  public String getPass() {
    return pass;
  }

  public void setPass(String pass) {
    this.pass = pass;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateUserExternal updateUserExternal = (UpdateUserExternal) o;
    return Objects.equals(this.admin, updateUserExternal.admin) &&
        Objects.equals(this.name, updateUserExternal.name) &&
        Objects.equals(this.pass, updateUserExternal.pass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(admin, name, pass);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateUserExternal {\n");
    
    sb.append("    admin: ").append(toIndentedString(admin)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    pass: ").append(toIndentedString(pass)).append("\n");
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
