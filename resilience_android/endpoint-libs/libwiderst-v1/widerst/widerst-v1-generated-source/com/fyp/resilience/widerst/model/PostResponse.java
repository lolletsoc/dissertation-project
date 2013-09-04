/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * Warning! This file is generated. Modify at your own risk.
 */

package com.fyp.resilience.widerst.model;

import com.google.api.client.json.GenericJson;

/**
 * Model definition for PostResponse.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the . For a detailed explanation see:
 * <a href="http://code.google.com/p/google-api-java-client/wiki/Json">http://code.google.com/p/google-api-java-client/wiki/Json</a>
 * </p>
 *
 * <p>
 * Upgrade warning: starting with version 1.12 {@code getResponseHeaders()} is removed, instead use
 * {@link com.google.api.client.http.json.JsonHttpRequest#getLastResponseHeaders()}
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class PostResponse extends GenericJson {

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_BUSY = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_NOT_REQUIRED = 3;
    public static final int STATUS_REGISTRATION_ERROR = 4;
    public static final int STATUS_WHOLE_COMPLETE = 5;
    
  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private String postUrl;

  /**
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private Integer success;

  /**

   * The value returned may be {@code null}.
   */
  public String getPostUrl() {
    return postUrl;
  }

  /**

   * The value set may be {@code null}.
   */
  public PostResponse setPostUrl(String postUrl) {
    this.postUrl = postUrl;
    return this;
  }

  /**

   * The value returned may be {@code null}.
   */
  public Integer getSuccess() {
    return success;
  }

  /**

   * The value set may be {@code null}.
   */
  public PostResponse setSuccess(Integer success) {
    this.success = success;
    return this;
  }

}
