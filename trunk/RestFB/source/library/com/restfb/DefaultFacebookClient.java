/*
 * Copyright (c) 2010 Mark Allen.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.restfb;

import static java.net.HttpURLConnection.HTTP_OK;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.restfb.WebRequestor.Response;
import com.restfb.json.JSONException;
import com.restfb.json.JSONObject;

/**
 * Default implementation of a Facebook API client.
 * 
 * @author <a href="http://restfb.com">Mark Allen</a>
 */
public class DefaultFacebookClient implements FacebookClient {
  /**
   * Facebook API key.
   */
  private String apiKey;

  /**
   * Facebook application secret key.
   */
  private String secretKey;

  /**
   * Handles {@code POST}s to the Facebook API endpoint.
   */
  private WebRequestor webRequestor;

  /**
   * Handles mapping Facebook response JSON to Java objects.
   */
  private JsonMapper jsonMapper;

  /**
   * Set of parameter names that user must not specify themselves, since we use
   * these parameters internally.
   */
  private Set<String> illegalParamNames;

  // Common parameter names/values that must be included in all API requests
  private static final String API_KEY_PARAM_NAME = "api_key";
  private static final String CALL_ID_PARAM_NAME = "call_id";
  private static final String SIG_PARAM_NAME = "sig";
  private static final String METHOD_PARAM_NAME = "method";
  private static final String SESSION_KEY_PARAM_NAME = "session_key";
  private static final String FORMAT_PARAM_NAME = "format";
  private static final String FORMAT_PARAM_VALUE = "json";
  private static final String VERSION_PARAM_NAME = "v";
  private static final String VERSION_PARAM_VALUE = "1.0";

  // API error response attributes
  private static final String ERROR_CODE_ATTRIBUTE_NAME = "error_code";
  private static final String ERROR_MSG_ATTRIBUTE_NAME = "error_msg";

  // API endpoint URL
  private static final String FACEBOOK_REST_ENDPOINT_URL =
      "http://api.facebook.com/restserver.php";

  private static final Logger logger =
      Logger.getLogger(DefaultFacebookClient.class);

  /**
   * Creates a Facebook API client with the given API key and secret key.
   * 
   * @param apiKey
   *          A Facebook API key.
   * @param secretKey
   *          A Facebook application secret key.
   * @throws NullPointerException
   *           If either parameter is {@code null}.
   * @throws IllegalArgumentException
   *           If either parameter is a blank string.
   */
  public DefaultFacebookClient(String apiKey, String secretKey) {
    this(apiKey, secretKey, new DefaultWebRequestor(), new DefaultJsonMapper());
  }

  /**
   * Creates a Facebook API client with the given API key and secret key.
   * 
   * @param apiKey
   *          A Facebook API key.
   * @param secretKey
   *          A Facebook application secret key.
   * @param webRequestor
   *          The {@link WebRequestor} implementation to use for {@code POST}ing
   *          to the API endpoint.
   * @param jsonMapper
   *          The {@link JsonMapper} implementation to use for mapping API
   *          response JSON to Java objects.
   * @throws NullPointerException
   *           If any parameter is {@code null}.
   * @throws IllegalArgumentException
   *           If either {@code apiKey} or {@code secretKey} is a blank string.
   */
  public DefaultFacebookClient(String apiKey, String secretKey,
      WebRequestor webRequestor, JsonMapper jsonMapper) {
    verifyParameterPresence("apiKey", apiKey);
    verifyParameterPresence("secretKey", secretKey);
    verifyParameterPresence("webRequestor", webRequestor);
    verifyParameterPresence("jsonMapper", jsonMapper);

    this.apiKey = apiKey;
    this.secretKey = secretKey;
    this.webRequestor = webRequestor;
    this.jsonMapper = jsonMapper;

    illegalParamNames = new HashSet<String>();
    illegalParamNames.addAll(Arrays.asList(new String[] { API_KEY_PARAM_NAME,
        CALL_ID_PARAM_NAME, SIG_PARAM_NAME, METHOD_PARAM_NAME,
        SESSION_KEY_PARAM_NAME, FORMAT_PARAM_NAME, VERSION_PARAM_NAME }));
  }

  /**
   * @see com.restfb.FacebookClient#execute(java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public void execute(String method, Parameter... parameters)
      throws FacebookException {
    execute(method, (String) null, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#execute(java.lang.String, java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public void execute(String method, String sessionKey, Parameter... parameters)
      throws FacebookException {
    makeRequest(method, sessionKey, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#execute(java.lang.String, java.lang.Class,
   *      com.restfb.Parameter[])
   */
  @Override
  public <T> T execute(String method, Class<T> resultType,
      Parameter... parameters) throws FacebookException {
    return execute(method, null, resultType, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#execute(java.lang.String, java.lang.String,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> T execute(String method, String sessionKey, Class<T> resultType,
      Parameter... parameters) throws FacebookException {
    return jsonMapper.toJavaObject(makeRequest(method, sessionKey, parameters),
      resultType);
  }

  /**
   * @see com.restfb.FacebookClient#executeForList(java.lang.String,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> List<T> executeForList(String method, Class<T> resultType,
      Parameter... parameters) throws FacebookException {
    return executeForList(method, null, resultType, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#executeForList(java.lang.String,
   *      java.lang.String, java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> List<T> executeForList(String method, String sessionKey,
      Class<T> resultType, Parameter... parameters) throws FacebookException {
    return jsonMapper.toJavaList(makeRequest(method, sessionKey, parameters),
      resultType);
  }

  protected String makeRequest(String method, String sessionKey,
      Parameter... parameters) throws FacebookException {
    // Make sure we're not provided with any params that conflict with what
    // we're passing to FB internally
    verifyParameterLegality(parameters);

    // Turn the set of parameters into a string per the API spec
    String parametersAsString =
        toParameterString(method, sessionKey, parameters);

    if (logger.isInfoEnabled())
      logger.info("POSTing to " + FACEBOOK_REST_ENDPOINT_URL + "?"
          + parametersAsString);

    Response response = null;

    // Perform a POST to the API endpoint
    try {
      response =
          webRequestor.executePost(FACEBOOK_REST_ENDPOINT_URL,
            parametersAsString);
    } catch (Throwable t) {
      throw new FacebookNetworkException("Facebook POST failed", t);
    }

    if (logger.isInfoEnabled())
      logger.info("Facebook responded with " + response);

    // If we get any HTTP response code other than a 200 OK, throw an exception
    if (HTTP_OK != response.getStatusCode())
      throw new FacebookNetworkException("Facebook POST failed", response
        .getStatusCode());

    String json = response.getBody();

    // If the response contained an error code, throw an exception
    throwFacebookResponseStatusExceptionIfNecessary(json);

    return json;
  }

  /**
   * If the {@code error_code} JSON field is present, we've got a response
   * status error for this API call. Extracts relevant information from the JSON
   * and throws a
   * 
   * @param json
   *          The JSON returned by Facebook in response to an API call.
   * @throws FacebookResponseStatusException
   *           If the JSON contains an error code.
   * @throws FacebookJsonMappingException
   *           If an error occurs while processing the JSON.
   */
  protected void throwFacebookResponseStatusExceptionIfNecessary(String json)
      throws FacebookResponseStatusException, FacebookJsonMappingException {
    try {
      // If this is an array and not an object, it's not an error response.
      if (json.startsWith("["))
        return;

      JSONObject errorObject = new JSONObject(json);
      if (!errorObject.has(ERROR_CODE_ATTRIBUTE_NAME))
        return;

      throw new FacebookResponseStatusException(errorObject
        .getString(ERROR_MSG_ATTRIBUTE_NAME), errorObject
        .getInt(ERROR_CODE_ATTRIBUTE_NAME));
    } catch (JSONException e) {
      throw new FacebookJsonMappingException("Unable to process JSON response",
        e);
    }
  }

  protected String toParameterString(String method, String sessionKey,
      Parameter... parameters) {
    Map<String, String> sortedParameters = new TreeMap<String, String>();
    sortedParameters.put(VERSION_PARAM_NAME, VERSION_PARAM_VALUE);
    sortedParameters.put(API_KEY_PARAM_NAME, apiKey);
    sortedParameters.put(CALL_ID_PARAM_NAME, String.valueOf(System
      .currentTimeMillis()));
    sortedParameters.put(FORMAT_PARAM_NAME, FORMAT_PARAM_VALUE);
    sortedParameters.put(METHOD_PARAM_NAME, method);

    if (!StringUtils.isBlank(sessionKey))
      sortedParameters.put(SESSION_KEY_PARAM_NAME, sessionKey);

    for (Parameter param : parameters)
      sortedParameters.put(param.name, param.value);

    sortedParameters.put(SIG_PARAM_NAME, generateSignature(sortedParameters));

    StringBuilder parameterStringBuilder = new StringBuilder();
    boolean first = true;

    for (Entry<String, String> entry : sortedParameters.entrySet()) {
      if (first)
        first = false;
      else
        parameterStringBuilder.append("&");

      parameterStringBuilder.append(StringUtils.urlEncode(entry.getKey()));
      parameterStringBuilder.append("=");
      parameterStringBuilder.append(StringUtils.urlEncode(entry.getValue()));
    }

    return parameterStringBuilder.toString();
  }

  protected String generateSignature(Map<String, String> sortedParameters) {
    StringBuilder parameterString = new StringBuilder();

    for (Entry<String, String> entry : sortedParameters.entrySet()) {
      parameterString.append(entry.getKey());
      parameterString.append("=");
      parameterString.append(entry.getValue());
    }

    parameterString.append(secretKey);
    return generateMd5(parameterString.toString());
  }

  protected String generateMd5(String string) {
    try {
      MessageDigest messageDigest = MessageDigest.getInstance("MD5");
      byte[] bytes = StringUtils.toBytes(string);

      StringBuilder result = new StringBuilder();
      for (byte b : messageDigest.digest(bytes)) {
        result.append(Integer.toHexString((b & 0xf0) >>> 4));
        result.append(Integer.toHexString(b & 0x0f));
      }
      return result.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected void verifyParameterLegality(Parameter... parameters) {
    for (Parameter parameter : parameters)
      if (illegalParamNames.contains(parameter.name))
        throw new IllegalArgumentException("Parameter '" + parameter.name
            + "' is reserved for RestFB use - "
            + "you cannot specify it yourself.");
  }

  protected void verifyParameterPresence(String parameterName, String parameter) {
    verifyParameterPresence(parameterName, (Object) parameter);
    if (parameter.trim().length() == 0)
      throw new IllegalArgumentException("The '" + parameterName
          + "' parameter cannot be an empty string.");
  }

  protected void verifyParameterPresence(String parameterName, Object parameter) {
    if (parameter == null)
      throw new NullPointerException("The '" + parameterName
          + "' parameter cannot be null.");
  }
}