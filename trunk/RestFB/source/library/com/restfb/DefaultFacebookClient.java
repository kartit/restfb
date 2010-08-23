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

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.logging.Level.INFO;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.restfb.WebRequestor.Response;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonException;
import com.restfb.json.JsonObject;
import com.restfb.util.StringUtils;

/**
 * Default implementation of a <a
 * href="http://developers.facebook.com/docs/api">Facebook Graph API</a> client.
 * 
 * @author <a href="http://restfb.com">Mark Allen</a>
 */
public class DefaultFacebookClient extends BaseFacebookClient implements
    FacebookClient {
  /**
   * Graph API access token.
   */
  protected String accessToken;

  /**
   * API endpoint URL.
   */
  protected static final String FACEBOOK_GRAPH_ENDPOINT_URL =
      "https://graph.facebook.com";

  /**
   * Legacy API endpoint URL, used to support FQL queries.
   */
  protected static final String FACEBOOK_LEGACY_ENDPOINT_URL =
      "https://api.facebook.com/method";

  /**
   * Reserved method override parameter name.
   */
  protected static final String METHOD_PARAM_NAME = "method";

  /**
   * Reserved "multiple IDs" parameter name.
   */
  protected static final String IDS_PARAM_NAME = "ids";

  /**
   * Reserved FQL query parameter name.
   */
  protected static final String QUERY_PARAM_NAME = "query";

  /**
   * Reserved FQL multiquery parameter name.
   */
  protected static final String QUERIES_PARAM_NAME = "queries";

  /**
   * Reserved "result format" parameter name.
   */
  protected static final String FORMAT_PARAM_NAME = "format";

  /**
   * API error response 'error' attribute name.
   */
  protected static final String ERROR_ATTRIBUTE_NAME = "error";

  /**
   * API error response 'type' attribute name.
   */
  protected static final String ERROR_TYPE_ATTRIBUTE_NAME = "type";

  /**
   * API error response 'message' attribute name.
   */
  protected static final String ERROR_MESSAGE_ATTRIBUTE_NAME = "message";

  /**
   * Creates a Facebook Graph API client with no access token.
   * <p>
   * Without an access token, you can view and search public graph data but
   * can't do much else.
   */
  public DefaultFacebookClient() {
    this(null);
  }

  /**
   * Creates a Facebook Graph API client with the given {@code accessToken}.
   * 
   * @param accessToken
   *          A Facebook OAuth2 access token.
   */
  public DefaultFacebookClient(String accessToken) {
    this(accessToken, new DefaultWebRequestor(), new DefaultJsonMapper());
  }

  /**
   * Creates a Facebook Graph API client with the given {@code accessToken},
   * {@code webRequestor}, and {@code jsonMapper}.
   * 
   * @param accessToken
   *          A Facebook OAuth2 access token.
   * @param webRequestor
   *          The {@link WebRequestor} implementation to use for sending
   *          requests to the API endpoint.
   * @param jsonMapper
   *          The {@link JsonMapper} implementation to use for mapping API
   *          response JSON to Java objects.
   * @throws NullPointerException
   *           If {@code jsonMapper} or {@code webRequestor} is {@code null}.
   */
  public DefaultFacebookClient(String accessToken, WebRequestor webRequestor,
      JsonMapper jsonMapper) {
    verifyParameterPresence("jsonMapper", jsonMapper);
    verifyParameterPresence("webRequestor", webRequestor);

    this.accessToken = StringUtils.trimToNull(accessToken);
    this.webRequestor = webRequestor;
    this.jsonMapper = jsonMapper;

    illegalParamNames.addAll(Arrays.asList(new String[] {
        ACCESS_TOKEN_PARAM_NAME, METHOD_PARAM_NAME, FORMAT_PARAM_NAME }));
  }

  /**
   * @see com.restfb.FacebookClient#deleteObject(java.lang.String)
   */
  @Override
  public boolean deleteObject(String object) throws FacebookException {
    verifyParameterPresence("object", object);
    return "true".equals(makeRequest(object, false, true, true, null));
  }

  /**
   * @see com.restfb.FacebookClient#fetchConnection(java.lang.String,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> Connection<T> fetchConnection(String connection,
      Class<T> connectionType, Parameter... parameters)
      throws FacebookException {
    verifyParameterPresence("connection", connection);
    verifyParameterPresence("connectionType", connectionType);
    return mapToConnection(makeRequest(connection, parameters), connectionType);
  }

  /**
   * @see com.restfb.FacebookClient#fetchConnectionPage(java.lang.String,
   *      java.lang.Class)
   */
  @Override
  public <T> Connection<T> fetchConnectionPage(final String connectionPageUrl,
      Class<T> connectionType) throws FacebookException {
    String connectionJson = makeRequestAndProcessResponse(new Requestor() {
      /**
       * @see com.restfb.DefaultFacebookClient.Requestor#makeRequest()
       */
      @Override
      public Response makeRequest() throws IOException {
        return webRequestor.executeGet(connectionPageUrl);
      }
    });

    return mapToConnection(connectionJson, connectionType);
  }

  protected <T> Connection<T> mapToConnection(String connectionJson,
      Class<T> connectionType) {
    List<T> data = new ArrayList<T>();
    String previous = null;
    String next = null;

    try {
      JsonObject jsonObject = new JsonObject(connectionJson);

      // Pull out data
      JsonArray jsonData = jsonObject.getJsonArray("data");
      for (int i = 0; i < jsonData.length(); i++)
        data.add(jsonMapper.toJavaObject(jsonData.get(i).toString(),
          connectionType));

      // Pull out paging info, if present
      if (jsonObject.has("paging")) {
        JsonObject jsonPaging = jsonObject.getJsonObject("paging");
        previous =
            jsonPaging.has("previous") ? jsonPaging.getString("previous")
                : null;
        next = jsonPaging.has("next") ? jsonPaging.getString("next") : null;
      }
    } catch (JsonException e) {
      throw new FacebookJsonMappingException(
        "Unable to map connection JSON to Java objects", e);
    }

    return new Connection<T>(data, previous, next);
  }

  /**
   * @see com.restfb.FacebookClient#fetchConnection(java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonObject fetchConnection(String connection, Parameter... parameters)
      throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#fetchConnectionPage(java.lang.String)
   */
  @Override
  public JsonObject fetchConnectionPage(String connectionPageUrl)
      throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#fetchObject(java.lang.String,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> T fetchObject(String object, Class<T> objectType,
      Parameter... parameters) throws FacebookException {
    verifyParameterPresence("object", object);
    verifyParameterPresence("objectType", objectType);
    return jsonMapper.toJavaObject(makeRequest(object, parameters), objectType);
  }

  /**
   * @see com.restfb.FacebookClient#fetchObject(java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonObject fetchObject(String object, Parameter... parameters)
      throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#fetchObjects(java.util.List,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> T fetchObjects(List<String> ids, Class<T> objectType,
      Parameter... parameters) throws FacebookException {
    verifyParameterPresence("ids", ids);
    verifyParameterPresence("connectionType", objectType);

    if (ids.size() == 0)
      throw new IllegalArgumentException("The list of IDs cannot be empty.");

    for (Parameter parameter : parameters)
      if (IDS_PARAM_NAME.equals(parameter.name))
        throw new IllegalArgumentException("You cannot specify the '"
            + IDS_PARAM_NAME + "' URL parameter yourself - "
            + "RestFB will populate this for you with "
            + "the list of IDs you passed to this method.");

    // Normalize the IDs
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i).trim().toLowerCase();
      if ("".equals(id))
        throw new IllegalArgumentException(
          "The list of IDs cannot contain blank strings.");
      ids.set(i, id);
    }

    try {
      JsonObject jsonObject =
          new JsonObject(
            makeRequest(
              "",
              parametersWithAdditionalParameter(
                Parameter.with(IDS_PARAM_NAME, StringUtils.join(ids)),
                parameters)));
      return jsonMapper.toJavaObject(jsonObject.toString(), objectType);
    } catch (JsonException e) {
      throw new FacebookJsonMappingException(
        "Unable to map connection JSON to Java objects", e);
    }
  }

  /**
   * @see com.restfb.FacebookClient#fetchObjects(java.util.List,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonObject fetchObjects(List<String> ids, Parameter... parameters)
      throws FacebookException {
    verifyParameterPresence("ids", ids);

    if (ids.size() == 0)
      throw new IllegalArgumentException("The list of IDs cannot be empty.");

    for (Parameter parameter : parameters)
      if (IDS_PARAM_NAME.equals(parameter.name))
        throw new IllegalArgumentException("You cannot specify the '"
            + IDS_PARAM_NAME + "' URL parameter yourself - "
            + "RestFB will populate this for you with "
            + "the list of IDs you passed to this method.");

    // Normalize the IDs
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i).trim().toLowerCase();
      if ("".equals(id))
        throw new IllegalArgumentException(
          "The list of IDs cannot contain blank strings.");
      ids.set(i, id);
    }

    try {
      return new JsonObject(makeRequest(
        "",
        parametersWithAdditionalParameter(
          Parameter.with(IDS_PARAM_NAME, StringUtils.join(ids)), parameters)));
    } catch (JsonException e) {
      throw new FacebookJsonMappingException("Unable to map JSON to Java.", e);
    }
  }

  /**
   * @see com.restfb.FacebookClient#publish(java.lang.String, java.lang.Class,
   *      java.io.InputStream, com.restfb.Parameter[])
   */
  @Override
  public <T> T publish(String connection, Class<T> objectType,
      InputStream binaryAttachment, Parameter... parameters)
      throws FacebookException {
    verifyParameterPresence("connection", connection);
    return jsonMapper
      .toJavaObject(
        makeRequest(connection, false, true, false, binaryAttachment,
          parameters), objectType);
  }

  /**
   * @see com.restfb.FacebookClient#publish(java.lang.String,
   *      java.io.InputStream, com.restfb.Parameter[])
   */
  @Override
  public JsonObject publish(String connection, InputStream binaryAttachment,
      Parameter... parameters) throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#publish(java.lang.String, java.lang.Class,
   *      com.restfb.Parameter[])
   */
  @Override
  public <T> T publish(String connection, Class<T> objectType,
      Parameter... parameters) throws FacebookException {
    return publish(connection, objectType, null, parameters);
  }

  /**
   * @see com.restfb.FacebookClient#publish(java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonObject publish(String connection, Parameter... parameters)
      throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#executeMultiquery(java.util.Map,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> T executeMultiquery(Map<String, String> queries,
      Class<T> objectType, Parameter... parameters) throws FacebookException {
    verifyParameterPresence("objectType", objectType);

    for (Parameter parameter : parameters)
      if (QUERIES_PARAM_NAME.equals(parameter.name))
        throw new IllegalArgumentException("You cannot specify the '"
            + QUERIES_PARAM_NAME + "' URL parameter yourself - "
            + "RestFB will populate this for you with "
            + "the queries you passed to this method.");

    try {
      JsonArray jsonArray =
          new JsonArray(makeRequest(
            "fql.multiquery",
            true,
            false,
            false,
            null,
            parametersWithAdditionalParameter(
              Parameter.with(QUERIES_PARAM_NAME, queriesToJson(queries)),
              parameters)));

      JsonObject normalizedJson = new JsonObject();

      for (int i = 0; i < jsonArray.length(); i++) {
        JsonObject jsonObject = jsonArray.getJsonObject(i);

        // For empty resultsets, Facebook will return an empty object instead of
        // an empty list. Hack around that here.
        JsonArray resultsArray =
            jsonObject.get("fql_result_set") instanceof JsonArray ? jsonObject
              .getJsonArray("fql_result_set") : new JsonArray();

        normalizedJson.put(jsonObject.getString("name"), resultsArray);
      }

      return jsonMapper.toJavaObject(normalizedJson.toString(), objectType);
    } catch (JsonException e) {
      throw new FacebookJsonMappingException(
        "Unable to process fql.multiquery JSON response", e);
    }
  }

  /**
   * @see com.restfb.FacebookClient#executeMultiquery(java.util.Map,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonObject executeMultiquery(Map<String, String> queries,
      Parameter... parameters) throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#executeQuery(java.lang.String,
   *      java.lang.Class, com.restfb.Parameter[])
   */
  @Override
  public <T> List<T> executeQuery(String query, Class<T> objectType,
      Parameter... parameters) throws FacebookException {
    verifyParameterPresence("query", query);
    verifyParameterPresence("objectType", objectType);

    for (Parameter parameter : parameters)
      if (QUERY_PARAM_NAME.equals(parameter.name))
        throw new IllegalArgumentException("You cannot specify the '"
            + QUERY_PARAM_NAME + "' URL parameter yourself - "
            + "RestFB will populate this for you with "
            + "the query you passed to this method.");

    return jsonMapper.toJavaList(
      makeRequest(
        "fql.query",
        true,
        false,
        false,
        null,
        parametersWithAdditionalParameter(
          Parameter.with(QUERY_PARAM_NAME, query), parameters)), objectType);
  }

  /**
   * @see com.restfb.FacebookClient#executeQuery(java.lang.String,
   *      com.restfb.Parameter[])
   */
  @Override
  public JsonArray executeQuery(String query, Parameter... parameters)
      throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * @see com.restfb.FacebookClient#convertSessionKeysToAccessTokens(java.lang.String,
   *      java.lang.String, java.lang.String[])
   */
  @Override
  public List<AccessToken> convertSessionKeysToAccessTokens(String apiKey,
      String secretKey, String... sessionKeys) throws FacebookException {
    throw new UnsupportedOperationException();
  }

  /**
   * Coordinates the process of executing the API request GET/POST and
   * processing the response we receive from the endpoint.
   * 
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part
   *          of the API call.
   * @return The JSON returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or
   *           processing the response.
   */
  protected String makeRequest(String endpoint, Parameter... parameters)
      throws FacebookException {
    return makeRequest(endpoint, false, false, false, null, parameters);
  }

  /**
   * Coordinates the process of executing the API request GET/POST and
   * processing the response we receive from the endpoint.
   * 
   * @param endpoint
   *          Facebook Graph API endpoint.
   * @param useLegacyEndpoint
   *          Should we hit the legacy endpoint ({@code true}) or the new Graph
   *          endpoint ({@code false})?
   * @param executeAsPost
   *          {@code true} to execute the web request as a {@code POST},
   *          {@code false} to execute as a {@code GET}.
   * @param executeAsDelete
   *          {@code true} to add a special 'treat this request as a
   *          {@code DELETE}' parameter.
   * @param binaryAttachment
   *          A binary file to include in a {@code POST} request. Pass
   *          {@code null} if no attachment should be sent.
   * @param parameters
   *          Arbitrary number of parameters to send along to Facebook as part
   *          of the API call.
   * @return The JSON returned by Facebook for the API call.
   * @throws FacebookException
   *           If an error occurs while making the Facebook API POST or
   *           processing the response.
   */
  protected String makeRequest(String endpoint, boolean useLegacyEndpoint,
      final boolean executeAsPost, boolean executeAsDelete,
      final InputStream binaryAttachment, Parameter... parameters)
      throws FacebookException {
    verifyParameterLegality(parameters);

    if (executeAsDelete)
      parameters =
          parametersWithAdditionalParameter(
            Parameter.with(METHOD_PARAM_NAME, "delete"), parameters);

    StringUtils.trimToEmpty(endpoint).toLowerCase();
    if (!endpoint.startsWith("/"))
      endpoint = "/" + endpoint;

    final String fullEndpoint =
        (useLegacyEndpoint ? getFacebookLegacyEndpointUrl()
            : getFacebookGraphEndpointUrl()) + endpoint;

    final String parameterString = toParameterString(parameters);

    return makeRequestAndProcessResponse(new Requestor() {
      /**
       * @see com.restfb.DefaultFacebookClient.Requestor#makeRequest()
       */
      @Override
      public Response makeRequest() throws IOException {
        return executeAsPost ? webRequestor.executePost(fullEndpoint,
          parameterString, binaryAttachment) : webRequestor
          .executeGet(fullEndpoint + "?" + parameterString);
      }
    });
  }

  protected static interface Requestor {
    Response makeRequest() throws IOException;
  }

  protected String makeRequestAndProcessResponse(Requestor requestor)
      throws FacebookException {
    Response response = null;

    // Perform a GET or POST to the API endpoint
    try {
      response = requestor.makeRequest();
    } catch (Throwable t) {
      throw new FacebookNetworkException("Facebook request failed", t);
    }

    if (logger.isLoggable(INFO))
      logger.info("Facebook responded with " + response);

    // If we get any HTTP response code other than a 200 OK or 400 Bad Request
    // or 401 Not Authorized or 500 Internal Server Error, throw an exception.
    // We handle 401s and 500s specially.
    if (HTTP_OK != response.getStatusCode()
        && HTTP_BAD_REQUEST != response.getStatusCode()
        && HTTP_UNAUTHORIZED != response.getStatusCode()
        && HTTP_INTERNAL_ERROR != response.getStatusCode())
      throw new FacebookNetworkException("Facebook request failed",
        response.getStatusCode());

    String json = response.getBody();

    // If the response contained an error code, throw an exception.
    // The response will usually have a 500 Internal Server Error or 401 Not
    // Authorized response code in this case.
    throwFacebookResponseStatusExceptionIfNecessary(json);

    // If there was no response error information and this was a 500 or 401
    // error, something weird happened on Facebook's end. Bail.
    if (HTTP_INTERNAL_ERROR == response.getStatusCode()
        || HTTP_UNAUTHORIZED == response.getStatusCode())
      throw new FacebookNetworkException("Facebook request failed",
        response.getStatusCode());

    return json;
  }

  /**
   * Throws an exception if Facebook returned an error response. Using the Graph
   * API, it's possible to see both the new Graph API-style errors as well as
   * Legacy API-style errors, so we have to handle both here. This method
   * extracts relevant information from the error JSON and throws an exception
   * which encapsulates it for end-user consumption.
   * <p>
   * For Graph API errors:
   * <p>
   * If the {@code error} JSON field is present, we've got a response status
   * error for this API call.
   * <p>
   * For Legacy errors (e.g. FQL):
   * <p>
   * If the {@code error_code} JSON field is present, we've got a response
   * status error for this API call.
   * 
   * @param json
   *          The JSON returned by Facebook in response to an API call.
   * @throws FacebookGraphException
   *           If the JSON contains a Graph API error response.
   * @throws FacebookResponseStatusException
   *           If the JSON contains an Legacy API error response.
   * @throws FacebookJsonMappingException
   *           If an error occurs while processing the JSON.
   */
  protected void throwFacebookResponseStatusExceptionIfNecessary(String json)
      throws FacebookResponseStatusException, FacebookGraphException,
      FacebookJsonMappingException {
    // If we have a legacy exception, throw it.
    throwLegacyFacebookResponseStatusExceptionIfNecessary(json);

    try {
      // If the result is not an object, bail immediately.
      if (!json.startsWith("{"))
        return;

      JsonObject errorObject = new JsonObject(json);

      if (errorObject == null || !errorObject.has(ERROR_ATTRIBUTE_NAME))
        return;

      JsonObject innerErrorObject =
          errorObject.getJsonObject(ERROR_ATTRIBUTE_NAME);

      throw new FacebookGraphException(
        innerErrorObject.getString(ERROR_TYPE_ATTRIBUTE_NAME),
        innerErrorObject.getString(ERROR_MESSAGE_ATTRIBUTE_NAME));
    } catch (JsonException e) {
      throw new FacebookJsonMappingException(
        "Unable to process the Facebook API response", e);
    }
  }

  /**
   * Generate the parameter string to be included in the Facebook API request.
   * 
   * @param parameters
   *          Arbitrary number of extra parameters to include in the request.
   * @return The parameter string to include in the Facebook API request.
   * @throws FacebookJsonMappingException
   *           If an error occurs when building the parameter string.
   */
  protected String toParameterString(Parameter... parameters)
      throws FacebookJsonMappingException {

    if (!StringUtils.isBlank(accessToken))
      parameters =
          parametersWithAdditionalParameter(
            Parameter.with(ACCESS_TOKEN_PARAM_NAME, accessToken), parameters);

    parameters =
        parametersWithAdditionalParameter(
          Parameter.with(FORMAT_PARAM_NAME, "json"), parameters);

    StringBuilder parameterStringBuilder = new StringBuilder();
    boolean first = true;

    for (Parameter parameter : parameters) {
      if (first)
        first = false;
      else
        parameterStringBuilder.append("&");

      parameterStringBuilder.append(StringUtils.urlEncode(parameter.name));
      parameterStringBuilder.append("=");
      parameterStringBuilder.append(urlEncodedValueForParameterName(
        parameter.name, parameter.value));
    }

    return parameterStringBuilder.toString();
  }

  /**
   * Returns the base endpoint URL for the Graph API.
   * 
   * @return The base endpoint URL for the Graph API.
   */
  protected String getFacebookGraphEndpointUrl() {
    return FACEBOOK_GRAPH_ENDPOINT_URL;
  }

  /**
   * Returns the base endpoint URL for the Old REST API.
   * 
   * @return The base endpoint URL for the Old REST API.
   */
  protected String getFacebookLegacyEndpointUrl() {
    return FACEBOOK_LEGACY_ENDPOINT_URL;
  }
}