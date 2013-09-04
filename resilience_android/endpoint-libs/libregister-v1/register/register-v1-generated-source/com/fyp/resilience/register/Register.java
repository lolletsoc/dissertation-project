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
 * This file was generated.
 *  with google-apis-code-generator 1.3.0 (build: 2013-04-12 22:39:29 UTC)
 *  on 2013-04-13 at 17:46:10 UTC 
 */

package com.fyp.resilience.register;

import com.google.api.client.googleapis.GoogleUtils;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.common.base.Preconditions;

/**
 * Service definition for Register (v1).
 *
 * <p>
 * Endpoint for clients to register for a unique ID abstracted from their GCM ID
 * </p>
 *
 * <p>
 * For more information about this service, see the
 * <a href="" target="_blank">API Documentation</a>
 * </p>
 *
 * <p>
 * This service uses {@link RegisterRequestInitializer} to initialize global parameters via its
 * {@link Builder}.
 * </p>
 *
 * <p>
 * Upgrade warning: this class now extends {@link AbstractGoogleJsonClient}, whereas in prior
 * version 1.8 it extended {@link com.google.api.client.googleapis.services.GoogleClient}.
 * </p>
 *
 * @since 1.3
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public class Register extends AbstractGoogleJsonClient {

  // Note: Leave this static initializer at the top of the file.
  static {
    Preconditions.checkState(GoogleUtils.VERSION.equals("1.13.2-beta"),
        "You are currently running with version %s of google-api-client. " +
        "You need version 1.13.2-beta of google-api-client to run version " +
        "1.13.2-beta of the  library.", GoogleUtils.VERSION);
  }

  /**
   * The default encoded root URL of the service. This is determined when the library is generated
   * and normally should not be changed.
   *
   * @since 1.7
   */
  public static final String DEFAULT_ROOT_URL = "https://resilience-fyp.appspot.com/_ah/api/";

  /**
   * The default encoded service path of the service. This is determined when the library is
   * generated and normally should not be changed.
   *
   * @since 1.7
   */
  public static final String DEFAULT_SERVICE_PATH = "register/v1/";

  /**
   * The default encoded base URL of the service. This is determined when the library is generated
   * and normally should not be changed.
   * @deprecated (scheduled to be removed in 1.13)
   */
  @Deprecated
  public static final String DEFAULT_BASE_URL = DEFAULT_ROOT_URL + DEFAULT_SERVICE_PATH;

  /**
   * Constructor.
   *
   * <p>
   * Use {@link Builder} if you need to specify any of the optional parameters.
   * </p>
   *
   * @param transport HTTP transport
   * @param jsonFactory JSON factory
   * @param httpRequestInitializer HTTP request initializer or {@code null} for none
   * @since 1.7
   */
  public Register(HttpTransport transport, JsonFactory jsonFactory,
      HttpRequestInitializer httpRequestInitializer) {
    super(transport,
        jsonFactory,
        DEFAULT_ROOT_URL,
        DEFAULT_SERVICE_PATH,
        httpRequestInitializer,
        false);
  }

  /**
   * @param transport HTTP transport
   * @param httpRequestInitializer HTTP request initializer or {@code null} for none
   * @param rootUrl root URL of the service
   * @param servicePath service path
   * @param jsonObjectParser JSON object parser
   * @param googleClientRequestInitializer Google request initializer or {@code null} for none
   * @param applicationName application name to be sent in the User-Agent header of requests or
   *        {@code null} for none
   * @param suppressPatternChecks whether discovery pattern checks should be suppressed on required
   *        parameters
   */
  Register(HttpTransport transport,
      HttpRequestInitializer httpRequestInitializer,
      String rootUrl,
      String servicePath,
      JsonObjectParser jsonObjectParser,
      GoogleClientRequestInitializer googleClientRequestInitializer,
      String applicationName,
      boolean suppressPatternChecks) {
    super(transport,
        httpRequestInitializer,
        rootUrl,
        servicePath,
        jsonObjectParser,
        googleClientRequestInitializer,
        applicationName,
        suppressPatternChecks);
  }

  @Override
  protected void initialize(AbstractGoogleClientRequest<?> httpClientRequest) throws java.io.IOException {
    super.initialize(httpClientRequest);
  }

  /**
   * An accessor for creating requests from the Devices collection.
   *
   * <p>The typical use is:</p>
   * <pre>
   *   {@code Register register = new Register(...);}
   *   {@code Register.Devices.List request = register.devices().list(parameters ...)}
   * </pre>
   *
   * @return the resource collection
   */
  public Devices devices() {
    return new Devices();
  }

  /**
   * The "devices" collection of methods.
   */
  public class Devices {

    /**
     * Create a request for the method "devices.delete".
     *
     * This request holds the parameters needed by the the register server.  After setting any optional
     * parameters, call the {@link Delete#execute()} method to invoke the remote operation.
     *
     * @param id
     * @return the request
     */
    public Delete delete(String id) throws java.io.IOException {
      Delete result = new Delete(id);
      initialize(result);
      return result;
    }

    public class Delete extends RegisterRequest<com.fyp.resilience.register.model.DeviceInfo> {

      private static final String REST_PATH = "devices/{id}";

      /**
       * Create a request for the method "devices.delete".
       *
       * This request holds the parameters needed by the the register server.  After setting any
       * optional parameters, call the {@link Delete#execute()} method to invoke the remote operation.
       * <p> {@link Delete#initialize(AbstractGoogleClientRequest)} must be called to initialize this
       * instance immediately after invoking the constructor. </p>
       *
       * @param id
       * @since 1.13
       */
      protected Delete(String id) {
        super(Register.this, "DELETE", REST_PATH, null, com.fyp.resilience.register.model.DeviceInfo.class);
        this.id = Preconditions.checkNotNull(id, "Required parameter id must be specified.");
      }

      @Override
      public Delete setAlt(String alt) {
        return (Delete) super.setAlt(alt);
      }

      @Override
      public Delete setFields(String fields) {
        return (Delete) super.setFields(fields);
      }

      @Override
      public Delete setKey(String key) {
        return (Delete) super.setKey(key);
      }

      @Override
      public Delete setOauthToken(String oauthToken) {
        return (Delete) super.setOauthToken(oauthToken);
      }

      @Override
      public Delete setPrettyPrint(Boolean prettyPrint) {
        return (Delete) super.setPrettyPrint(prettyPrint);
      }

      @Override
      public Delete setQuotaUser(String quotaUser) {
        return (Delete) super.setQuotaUser(quotaUser);
      }

      @Override
      public Delete setUserIp(String userIp) {
        return (Delete) super.setUserIp(userIp);
      }

      @com.google.api.client.util.Key
      private String id;

      /**

       */
      public String getId() {
        return id;
      }

      public Delete setId(String id) {
        this.id = id;
        return this;
      }

    }
    /**
     * Create a request for the method "devices.insert".
     *
     * This request holds the parameters needed by the the register server.  After setting any optional
     * parameters, call the {@link Insert#execute()} method to invoke the remote operation.
     *
     * @param content the {@link com.fyp.resilience.register.model.DeviceInfo}
     * @return the request
     */
    public Insert insert(com.fyp.resilience.register.model.DeviceInfo content) throws java.io.IOException {
      Insert result = new Insert(content);
      initialize(result);
      return result;
    }

    public class Insert extends RegisterRequest<com.fyp.resilience.register.model.DeviceInfo> {

      private static final String REST_PATH = "devices";

      /**
       * Create a request for the method "devices.insert".
       *
       * This request holds the parameters needed by the the register server.  After setting any
       * optional parameters, call the {@link Insert#execute()} method to invoke the remote operation.
       * <p> {@link Insert#initialize(AbstractGoogleClientRequest)} must be called to initialize this
       * instance immediately after invoking the constructor. </p>
       *
       * @param content the {@link com.fyp.resilience.register.model.DeviceInfo}
       * @since 1.13
       */
      protected Insert(com.fyp.resilience.register.model.DeviceInfo content) {
        super(Register.this, "POST", REST_PATH, content, com.fyp.resilience.register.model.DeviceInfo.class);
      }

      @Override
      public Insert setAlt(String alt) {
        return (Insert) super.setAlt(alt);
      }

      @Override
      public Insert setFields(String fields) {
        return (Insert) super.setFields(fields);
      }

      @Override
      public Insert setKey(String key) {
        return (Insert) super.setKey(key);
      }

      @Override
      public Insert setOauthToken(String oauthToken) {
        return (Insert) super.setOauthToken(oauthToken);
      }

      @Override
      public Insert setPrettyPrint(Boolean prettyPrint) {
        return (Insert) super.setPrettyPrint(prettyPrint);
      }

      @Override
      public Insert setQuotaUser(String quotaUser) {
        return (Insert) super.setQuotaUser(quotaUser);
      }

      @Override
      public Insert setUserIp(String userIp) {
        return (Insert) super.setUserIp(userIp);
      }

    }
    /**
     * Create a request for the method "devices.update".
     *
     * This request holds the parameters needed by the the register server.  After setting any optional
     * parameters, call the {@link Update#execute()} method to invoke the remote operation.
     *
     * @param content the {@link com.fyp.resilience.register.model.DeviceInfo}
     * @return the request
     */
    public Update update(com.fyp.resilience.register.model.DeviceInfo content) throws java.io.IOException {
      Update result = new Update(content);
      initialize(result);
      return result;
    }

    public class Update extends RegisterRequest<com.fyp.resilience.register.model.DeviceInfo> {

      private static final String REST_PATH = "devices";

      /**
       * Create a request for the method "devices.update".
       *
       * This request holds the parameters needed by the the register server.  After setting any
       * optional parameters, call the {@link Update#execute()} method to invoke the remote operation.
       * <p> {@link Update#initialize(AbstractGoogleClientRequest)} must be called to initialize this
       * instance immediately after invoking the constructor. </p>
       *
       * @param content the {@link com.fyp.resilience.register.model.DeviceInfo}
       * @since 1.13
       */
      protected Update(com.fyp.resilience.register.model.DeviceInfo content) {
        super(Register.this, "PUT", REST_PATH, content, com.fyp.resilience.register.model.DeviceInfo.class);
      }

      @Override
      public Update setAlt(String alt) {
        return (Update) super.setAlt(alt);
      }

      @Override
      public Update setFields(String fields) {
        return (Update) super.setFields(fields);
      }

      @Override
      public Update setKey(String key) {
        return (Update) super.setKey(key);
      }

      @Override
      public Update setOauthToken(String oauthToken) {
        return (Update) super.setOauthToken(oauthToken);
      }

      @Override
      public Update setPrettyPrint(Boolean prettyPrint) {
        return (Update) super.setPrettyPrint(prettyPrint);
      }

      @Override
      public Update setQuotaUser(String quotaUser) {
        return (Update) super.setQuotaUser(quotaUser);
      }

      @Override
      public Update setUserIp(String userIp) {
        return (Update) super.setUserIp(userIp);
      }

    }

  }

  /**
   * Builder for {@link Register}.
   *
   * <p>
   * Implementation is not thread-safe.
   * </p>
   *
   * @since 1.3.0
   */
  public static final class Builder extends AbstractGoogleJsonClient.Builder {

    /**
     * Returns an instance of a new builder.
     *
     * @param transport HTTP transport
     * @param jsonFactory JSON factory
     * @param httpRequestInitializer HTTP request initializer or {@code null} for none
     * @since 1.7
     */
    public Builder(HttpTransport transport, JsonFactory jsonFactory,
        HttpRequestInitializer httpRequestInitializer) {
      super(
          transport,
          jsonFactory,
          DEFAULT_ROOT_URL,
          DEFAULT_SERVICE_PATH,
          httpRequestInitializer,
          false);
    }

    /** Builds a new instance of {@link Register}. */
    @Override
    public Register build() {
      return new Register(getTransport(),
          getHttpRequestInitializer(),
          getRootUrl(),
          getServicePath(),
          getObjectParser(),
          getGoogleClientRequestInitializer(),
          getApplicationName(),
          getSuppressPatternChecks());
    }

    @Override
    public Builder setRootUrl(String rootUrl) {
      return (Builder) super.setRootUrl(rootUrl);
    }

    @Override
    public Builder setServicePath(String servicePath) {
      return (Builder) super.setServicePath(servicePath);
    }

    @Override
    public Builder setHttpRequestInitializer(HttpRequestInitializer httpRequestInitializer) {
      return (Builder) super.setHttpRequestInitializer(httpRequestInitializer);
    }

    @Override
    public Builder setApplicationName(String applicationName) {
      return (Builder) super.setApplicationName(applicationName);
    }

    @Override
    public Builder setSuppressPatternChecks(boolean suppressPatternChecks) {
      return (Builder) super.setSuppressPatternChecks(suppressPatternChecks);
    }

    /**
     * Set the {@link RegisterRequestInitializer}.
     *
     * @since 1.12
     */
    public Builder setRegisterRequestInitializer(
        RegisterRequestInitializer registerRequestInitializer) {
      return (Builder) super.setGoogleClientRequestInitializer(registerRequestInitializer);
    }

    @Override
    public Builder setGoogleClientRequestInitializer(
        GoogleClientRequestInitializer googleClientRequestInitializer) {
      return (Builder) super.setGoogleClientRequestInitializer(googleClientRequestInitializer);
    }
  }
}
