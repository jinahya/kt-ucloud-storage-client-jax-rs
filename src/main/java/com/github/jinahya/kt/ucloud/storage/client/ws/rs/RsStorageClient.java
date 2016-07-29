/*
 * Copyright 2016 Jin Kwon &lt;onacit_at_gmail.com&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jinahya.kt.ucloud.storage.client.ws.rs;

import java.util.Collection;
import static java.util.Collections.singletonList;
import java.util.Date;
import java.util.List;
import static java.util.Optional.ofNullable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * A client using JAX-RS.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
public class RsStorageClient {

    private static final Logger logger
            = getLogger(RsStorageClient.class.getName());

    public static final String AUTH_URL_STANDARD_KOR_CENTER
            = "https://api.ucloudbiz.olleh.com/storage/v1/auth";

    public static final String AUTH_URL_STANDARD_JPN
            = "https://api.ucloudbiz.olleh.com/storage/v1/authjp";

    public static final String AUTH_URL_LITE_KOR_HA
            = "https://api.ucloudbiz.olleh.com/storage/v1/authlite";

    public static final String HEADER_X_AUTH_USER = "X-Storage-User";

    public static final String HEADER_X_AUTH_PASS = "X-Storage-Pass";

    public static final String HEADER_X_AUTH_NEW_TOKEN = "X-Auth-New-Token";

    public static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";

    public static final String HEADER_X_AUTH_TOKEN_EXPIRES
            = "X-Auth-Token-Expires";

    public static final String HEADER_X_STORAGE_URL = "X-Storage-Url";

    /**
     * Authenticates with given arguments.
     *
     * @param client the client
     * @param authUrl authentication URL
     * @param authUser authentication username
     * @param authPass authentication password
     * @return a response
     */
    public static Response authenticateUser(final Client client,
                                            final String authUrl,
                                            final String authUser,
                                            final String authPass) {
        return client
                .target(authUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header(HEADER_X_AUTH_USER, authUser) // X-Auth-User -> 500
                .header(HEADER_X_AUTH_PASS, authPass) // X-Auth-Pass -> 500
                .header(HEADER_X_AUTH_NEW_TOKEN, true)
                .buildGet()
                .invoke();
    }

    public static WebTarget targetContainer(
            final Client client, final String storageUrl,
            final String containerName,
            final MultivaluedMap<String, Object> params) {
        logger.log(Level.INFO, "targetContainer({0}, {1}, {2}, {3})",
                   new Object[]{client, storageUrl, containerName, params});
        final WebTarget target = client.target(storageUrl).path(containerName);
        if (params != null) {
            params.forEach((name, values) -> target.queryParam(name, values));
        }
        return target;
    }

    /**
     * Targets container.
     *
     * @param client the client to use.
     * @param storageUrl the storage URL
     * @param containerName container name
     * @return a target.
     * @deprecated Use
     * {@link #targetContainer(javax.ws.rs.client.Client, java.lang.String, java.lang.String, javax.ws.rs.core.MultivaluedMap)}
     */
    @Deprecated
    public static WebTarget targetContainer(final Client client,
                                            final String storageUrl,
                                            final String containerName) {
        return targetContainer(client, storageUrl, containerName, null);
    }

    public static Invocation.Builder buildContainer(
            final Client client, final String storageUrl,
            final String containerName,
            final MultivaluedMap<String, Object> params,
            final String authToken) {
        logger.log(Level.INFO, "buildContainer({0}, {1}, {2}, {3}, {4})",
                   new Object[]{client, storageUrl, containerName, params,
                                authToken});
        return targetContainer(client, storageUrl, containerName, params)
                .request()
                .header(HEADER_X_AUTH_TOKEN, authToken);
    }

    /**
     * Builds an invocation for a container.
     *
     * @param client the client
     * @param storageUrl the storage URL
     * @param containerName container name
     * @param authToken authentication token.
     * @return an invocation builder.
     * @deprecated Use
     * {@link #buildContainer(javax.ws.rs.client.Client, java.lang.String, java.lang.String, javax.ws.rs.core.MultivaluedHashMap, java.lang.String)}
     */
    @Deprecated
    public static Invocation.Builder buildContainer(
            final Client client, final String storageUrl,
            final String containerName, final String authToken) {
        return buildContainer(client, storageUrl, containerName, null,
                              authToken);
    }

    public static WebTarget targetObject(
            final Client client, final String storageUrl,
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> params) {
        final WebTarget target
                = client.target(storageUrl).path(containerName)
                .path(objectName);
        if (params != null) {
            params.forEach((name, values) -> target.queryParam(name, values));
        }
//        final WebTarget target = targetContainer(
//                client, storageUrl, containerName, params).path(objectName);
        return target;
    }

    /**
     * Targets an object.
     *
     * @param client the client
     * @param storageUrl the storage URL
     * @param containerName the container name
     * @param objectName the object name
     * @return a target.
     * @deprecated Use
     * {@link #targetObject(javax.ws.rs.client.Client, java.lang.String, java.lang.String, java.lang.String, javax.ws.rs.core.MultivaluedMap)}
     */
    @Deprecated
    public static WebTarget targetObject(final Client client,
                                         final String storageUrl,
                                         final String containerName,
                                         final String objectName) {
        return targetObject(client, storageUrl, containerName, objectName,
                            null);
    }

    /**
     * Builds an invocation for an object.
     *
     * @param client the client
     * @param storageUrl the storage URL
     * @param containerName the container name
     * @param objectName the object name
     * @param authToken the authentication token
     * @return an invocation builder.
     */
    public static Invocation.Builder buildObject(
            final Client client, final String storageUrl,
            final String containerName, final String objectName,
            final String authToken) {
        return targetObject(client, storageUrl, containerName, objectName)
                .request()
                .header(HEADER_X_AUTH_TOKEN, authToken);
    }

    /**
     * Creates a new instance.
     *
     * @param authUrl the authentication URL
     * @param authUser the authentication username
     * @param authPass the authentication password
     */
    public RsStorageClient(final String authUrl, final String authUser,
                           final String authPass) {
        super();
        this.authUrl = authUrl;
        this.authUser = authUser;
        this.authPass = authPass;
    }

    /**
     * Authenticates user and applies given function with the response.
     *
     * @param <T> return value type parameter.
     * @param function the function to be applied with an response.
     * @return the value applied
     */
    public <T> T authenticateUser(final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            final Response response = authenticateUser(
                    client, authUrl, authUser, authPass);
            try {
                final int statusCode = response.getStatus();
                if (statusCode == Status.OK.getStatusCode()
                    || statusCode == Status.NO_CONTENT.getStatusCode()) {
                    storageUrl = response.getHeaderString(HEADER_X_STORAGE_URL);
                    assert storageUrl != null;
                    authToken = response.getHeaderString(HEADER_X_AUTH_TOKEN);
                    assert authToken != null;
                    final String expires = response.getHeaderString(
                            HEADER_X_AUTH_TOKEN_EXPIRES);
                    assert expires != null;
                    tokenExpires = new Date(
                            System.currentTimeMillis()
                            + (Long.parseLong(expires) * 1000L));
                }
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T authenticateUser(
            final BiFunction<RsStorageClient, Response, T> function) {
        return authenticateUser(response -> function.apply(this, response));
    }

    /**
     * Checks if the authentication token is valid before specified
     * milliseconds.
     *
     * @param millis the milliseconds.
     * @return {@code true} if the token is valid; {@code false} otherwise.
     */
    public boolean validBefore(final long millis) {
        return authToken != null && tokenExpires != null
               && tokenExpires.getTime() >= millis;
    }

    public <T> T refreshToken(final long millis,
                              final Function<Response, T> function) {
        if (!validBefore(millis)) {
            return authenticateUser(function);
        }
        return function.apply(null);
    }

    public <T> T refreshToken(
            final long millis,
            final BiFunction<RsStorageClient, Response, T> function) {
        if (!validBefore(millis)) {
            return authenticateUser(function);
        }
        return function.apply(this, null);
    }

    /**
     * Reads container.
     *
     * @param <T> result type parameter
     * @param containerName container name
     * @param params query parameters
     * @param headers request headers.
     * @param function the function to be applied with the response
     * @return the value the function results.
     */
    public <T> T readContainer(final String containerName,
                               final MultivaluedMap<String, Object> params,
                               final MultivaluedMap<String, Object> headers,
                               final Function<Response, T> function) {
        authenticateUser(response -> null);
        final Client client = ClientBuilder.newClient();
        try {
            client.register((ClientRequestFilter) requestContext -> {
                System.out.println("---> request.method: " + requestContext.getMethod());
                System.out.println("---> request.uri: " + requestContext.getUri());
                System.out.println("---> request.headers: " + requestContext.getHeaders());
                System.out.println("---> request.entity: " + requestContext.getEntity());
            });
            final Invocation.Builder builder = buildContainer(
                    client, storageUrl, containerName, params, authToken);
            if (headers != null) {
//                final List<Object> accepts = headers.remove(HttpHeaders.ACCEPT);
//                ofNullable(accepts).filter(Collection::isEmpty).ifPresent(v -> builder.accept(v.get(0).toString()));
                headers.putSingle(HEADER_X_AUTH_TOKEN, singletonList(authToken));
                logger.info("--> headers: " + headers);
                builder.headers(headers);
            }
            logger.log(Level.INFO, "builder: {0}", builder);
            final Invocation invocation = builder.buildGet();
//            final Response response = builder.get();
            final Response response = invocation.invoke();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    /**
     * Updates container.
     *
     * @param <T> return value type parameter
     * @param containerName the container name
     * @param headers additional request headers
     * @param function the function to be applied with the response.
     * @return the value applied.
     * @deprecated
     */
    @Deprecated
    public <T> T updateContainer(final String containerName,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            client.register((ClientRequestFilter) requestContext -> {
                System.out.println("---> request.method: " + requestContext.getMethod());
                System.out.println("---> request.uri: " + requestContext.getUri());
                System.out.println("---> request.headers: " + requestContext.getHeaders());
            });
            final Invocation.Builder builder = buildContainer(
                    client, storageUrl, containerName, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Response response = builder
                    .put(Entity.entity(
                            new byte[0], MediaType.APPLICATION_OCTET_STREAM));
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T updateContainer(final String containerName,
                                 final MultivaluedMap<String, Object> params,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            client.register((ClientRequestFilter) requestContext -> {
                System.out.println("---> request.method: " + requestContext.getMethod());
                System.out.println("---> request.uri: " + requestContext.getUri());
                System.out.println("---> request.headers: " + requestContext.getHeaders());
            });
            final Invocation.Builder builder = buildContainer(
                    client, storageUrl, containerName, params, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Response response = builder
                    .put(Entity.entity(
                            new byte[0], MediaType.APPLICATION_OCTET_STREAM));
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    /**
     * Creates or updates the container identified by given name.
     *
     * @param <T> return value type parameter
     * @param containerName the container name
     * @param headers additional request headers
     * @param function the function to be applied with the web response
     * @return the value function results
     */
    public <T> T updateContainer(
            final String containerName,
            final MultivaluedMap<String, Object> headers,
            final BiFunction<RsStorageClient, Response, T> function) {
        return updateContainer(containerName, headers,
                               response -> function.apply(this, response));
    }

    /**
     * Deletes a container identified by given name and returns the result .
     *
     * @param <T> return value type parameter
     * @param containerName the container name
     * @param headers additional request headers
     * @param function the function to be applied with the response.
     * @return the value function results.
     */
    public <T> T deleteContainer(final String containerName,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            final Invocation.Builder builder = buildContainer(
                    client, storageUrl, containerName, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Response response = builder.delete();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T deleteContainer(
            final String containerName,
            final MultivaluedMap<String, Object> headers,
            final BiFunction<RsStorageClient, Response, T> function) {
        return deleteContainer(containerName, headers,
                               response -> function.apply(this, response));
    }

    // ------------------------------------------------------------------ object
    public <T> T readObject(final String containerName, final String objectName,
                            final MultivaluedMap<String, Object> headers,
                            final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            final Invocation.Builder builder = buildObject(
                    client, storageUrl, containerName, objectName, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Invocation invocation = builder.buildGet();
            final Response response = invocation.invoke();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T readObject(
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> headers,
            final BiFunction<RsStorageClient, Response, T> function) {
        return readObject(containerName, objectName, headers,
                          response -> function.apply(this, response));
    }

    /**
     * Updates an object.
     *
     * @param <T> return value type parameter
     * @param containerName the container name
     * @param objectName the object name
     * @param headers additional request headers; may be {@code null}
     * @param entity the entity
     * @param function a function applies with the response.
     * @return a value the function results.
     */
    public <T> T updateObject(final String containerName,
                              final String objectName,
                              final MultivaluedMap<String, Object> headers,
                              final Entity<?> entity,
                              final Function<Response, T> function) {
        updateContainer(containerName, null, response -> null);
        final Client client = ClientBuilder.newClient();
        try {
            final Invocation.Builder builder = buildObject(
                    client, storageUrl, containerName, objectName, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Invocation invocation = builder.buildPut(entity);
            final Response response = invocation.invoke();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T updateObject(
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> headers,
            final Entity<?> entity,
            final BiFunction<RsStorageClient, Response, T> function) {
        return updateObject(containerName, objectName, headers, entity,
                            response -> function.apply(this, response));
    }

    /**
     * Deletes an object.
     *
     * @param <T> return value type parameter
     * @param containerName the container name
     * @param objectName the object name
     * @param headers additional headers; may be {@code null}.
     * @param function a function applies with the response.
     * @return a value the function results
     */
    public <T> T deleteObject(final String containerName,
                              final String objectName,
                              final MultivaluedMap<String, Object> headers,
                              final Function<Response, T> function) {
        final Client client = ClientBuilder.newClient();
        try {
            final Invocation.Builder builder = buildObject(
                    client, storageUrl, containerName, objectName, authToken);
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, authToken);
                builder.headers(headers);
            }
            final Invocation invocation = builder.buildDelete();
            final Response response = invocation.invoke();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    public <T> T deleteObject(
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> headers,
            final BiFunction<RsStorageClient, Response, T> function) {
        return deleteObject(containerName, objectName, headers,
                            response -> function.apply(this, response));
    }

    // -------------------------------------------------------------- storageUrl
    public String getStorageUrl() {
        return storageUrl;
    }

    // ------------------------------------------------------------ tokenExpires
    public Date getTokenExpires() {
        if (tokenExpires == null) {
            return null;
        }
        return new Date(tokenExpires.getTime());
    }

    private final String authUrl;

    private final String authUser;

    private final String authPass;

    private String storageUrl;

    private String authToken;

    private Date tokenExpires;
}
