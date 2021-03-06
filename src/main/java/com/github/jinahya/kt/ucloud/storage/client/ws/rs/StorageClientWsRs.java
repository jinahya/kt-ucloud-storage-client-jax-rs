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

import com.github.jinahya.kt.ucloud.storage.client.StorageClient;
import com.github.jinahya.kt.ucloud.storage.client.StorageClientException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import static java.lang.invoke.MethodHandles.lookup;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import static javax.ws.rs.core.Response.Status.OK;
import javax.ws.rs.core.Response.StatusType;

/**
 * A client for accessing kt ucloud storage using classes in
 * {@code javax.ws.rs}.
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
public class StorageClientWsRs
        extends StorageClient<StorageClientWsRs, Invocation.Builder, Response> {

    private static final Logger logger
            = getLogger(lookup().lookupClass().getName());

    /**
     * Returns a newly created instance of {@link MultivaluedMap} containing
     * given map's entries.
     *
     * @param <K> key type parameter
     * @param <V> value type parameter
     * @param map the map
     * @return a {@link MultivaluedMap} containing given map's entries or
     * {@code null} if the {@code map} is {@code null}
     */
    public static <K, V> MultivaluedMap<K, V> multivalued(
            final Map<K, List<V>> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof MultivaluedMap) {
            return (MultivaluedMap<K, V>) map;
        }
        final MultivaluedMap<K, V> multi = new MultivaluedHashMap<>(map.size());
        multi.putAll(map);
        return multi;
    }

    public static <R> R statusInfo(final Response response,
                                   final Function<StatusType, R> function) {
        return function.apply(response.getStatusInfo());
    }

    public static Family statusFamily(final Response response) {
        return statusInfo(response, StatusType::getFamily);
    }

    public static int statusCode(final Response response) {
        return statusInfo(response, StatusType::getStatusCode);
    }

    @Deprecated
    public static String reasonPhrase(final Response response) {
        return statusInfo(response, StatusType::getReasonPhrase);
    }

    // -------------------------------------------------------------------------
    /**
     * Put {@link #HEADER_X_AUTH_USER} and {@link #HEADER_X_AUTH_KEY} on given
     * builder.
     *
     * @param builder the builder
     * @param user the value for {@link #HEADER_X_AUTH_USER}
     * @param key the value for {@link #HEADER_X_AUTH_KEY}
     * @return given builder.
     * @see Invocation.Builder#header(java.lang.String, java.lang.Object)
     */
    public static Invocation.Builder auth(final Invocation.Builder builder,
                                          final String user, final String key) {
        return builder
                .header(HEADER_X_AUTH_USER, user)
                .header(HEADER_X_AUTH_KEY, key);
    }

    /**
     * Put {@link #HEADER_X_AUTH_USER} and {@link #HEADER_X_AUTH_KEY} on given
     * map.
     *
     * @param headers the map
     * @param user the value for {@link #HEADER_X_AUTH_USER}
     * @param key the value for {@link #HEADER_X_AUTH_KEY}
     * @return given map.
     * @see MultivaluedMap#putSingle(java.lang.Object, java.lang.Object)
     */
    public static MultivaluedMap<String, Object> auth(
            final MultivaluedMap<String, Object> headers,
            final String user, final String key) {
        headers.putSingle(HEADER_X_AUTH_USER, user);
        headers.putSingle(HEADER_X_AUTH_KEY, key);
        return headers;
    }

    /**
     * Put {@link #HEADER_X_AUTH_ADMIN_USER} and
     * {@link #HEADER_X_AUTH_ADMIN_KEY} on given builder.
     *
     * @param builder the builder
     * @param user the value for {@link #HEADER_X_AUTH_ADMIN_USER}
     * @param key the value for {@link #HEADER_X_AUTH_ADMIN_KEY}
     * @return given builder
     * @see Invocation.Builder#header(java.lang.String, java.lang.Object)
     */
    public static Invocation.Builder authAdmin(final Invocation.Builder builder,
                                               final String user,
                                               final String key) {
        return builder
                .header(HEADER_X_AUTH_ADMIN_USER, user)
                .header(HEADER_X_AUTH_ADMIN_KEY, key);
    }

    /**
     * Put {@link #HEADER_X_AUTH_ADMIN_USER} and
     * {@link #HEADER_X_AUTH_ADMIN_KEY} on given map.
     *
     * @param headers the map
     * @param user the value for {@link #HEADER_X_AUTH_ADMIN_USER}
     * @param key the value for {@link #HEADER_X_AUTH_ADMIN_KEY}
     * @return given map
     * @see MultivaluedMap#putSingle(java.lang.Object, java.lang.Object)
     */
    public static MultivaluedMap<String, Object> authAdmin(
            final MultivaluedMap<String, Object> headers,
            final String user, final String key) {
        headers.putSingle(HEADER_X_AUTH_ADMIN_USER, user);
        headers.putSingle(HEADER_X_AUTH_ADMIN_KEY, key);
        return headers;
    }

    public static Response authenticateUser(final Client client,
                                            final String authUrl,
                                            final String authUser,
                                            final String authKey,
                                            final boolean newToken) {
        Invocation.Builder builder = client
                .target(authUrl)
                .request()
                .header(HEADER_X_AUTH_USER, authUser)
                .header(HEADER_X_AUTH_KEY, authKey);
        if (newToken) {
            builder = builder.header(HEADER_X_AUTH_NEW_TOKEN, Boolean.TRUE);
        }
        return builder.buildGet().invoke();
    }

    // ---------------------------------------------------------------- /storage
    /**
     * Targets an account.
     *
     * @param client a client to use
     * @param storageUrl storage URL
     * @param params query parameters; may be {@code null}
     * @return a web target
     */
    public static WebTarget targetStorage(
            final Client client, final String storageUrl,
            final MultivaluedMap<String, Object> params) {
        WebTarget target = client.target(storageUrl);
        if (params != null) {
            for (final Entry<String, List<Object>> entry : params.entrySet()) {
                final String name = entry.getKey();
                final Object[] values = entry.getValue().toArray();
                target = target.queryParam(name, values);
            }
        }
        return target;
    }

    /**
     * Builds for an account.
     *
     * @param client a client to use
     * @param storageUrl a storage URL
     * @param params query parameters; may be {@code null}
     * @param authToken an authorization token
     * @return a builder.
     */
    public static Invocation.Builder buildStorage(
            final Client client, final String storageUrl,
            final MultivaluedMap<String, Object> params,
            final String authToken) {
        return targetStorage(client, storageUrl, params)
                .request()
                .header(HEADER_X_AUTH_TOKEN, authToken);
    }

    // ------------------------------------------------------ /storage/container
    /**
     * Targets a container.
     *
     * @param client a client
     * @param storageUrl storage URL
     * @param containerName container name
     * @param params query parameters
     * @return a target
     */
    public static WebTarget targetContainer(
            final Client client, final String storageUrl,
            final String containerName,
            final MultivaluedMap<String, Object> params) {
        if (ThreadLocalRandom.current().nextBoolean()) {
            return targetStorage(client, storageUrl, params)
                    .path(containerName);
        }
        WebTarget target = client.target(storageUrl).path(containerName);
        if (params != null) {
            for (final Entry<String, List<Object>> entry : params.entrySet()) {
                final String name = entry.getKey();
                final Object[] values = entry.getValue().toArray();
                target = target.queryParam(name, values);
            }
        }
        return target;
    }

    /**
     * Builds for a container.
     *
     * @param client a client to use
     * @param storageUrl storage URL
     * @param containerName container name
     * @param params query parameters; may be {@code null}
     * @param authToken an authorization token
     * @return a builder.
     */
    public static Invocation.Builder buildContainer(
            final Client client, final String storageUrl,
            final String containerName,
            final MultivaluedMap<String, Object> params,
            final String authToken) {
        return targetContainer(client, storageUrl, containerName, params)
                .request()
                .header(HEADER_X_AUTH_TOKEN, authToken);
    }

    // ----------------------------------------------- /storage/container/object
    /**
     * Targets an object.
     *
     * @param client a client
     * @param storageUrl the storage URL
     * @param containerName container name
     * @param objectName object name
     * @param params query parameters
     * @return a target
     */
    public static WebTarget targetObject(
            final Client client, final String storageUrl,
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> params) {
        WebTarget target
                = targetContainer(client, storageUrl, containerName, params)
                .path(objectName);
        if (params != null) {
            for (final Entry<String, List<Object>> entry : params.entrySet()) {
                final String name = entry.getKey();
                final Object[] values = entry.getValue().toArray();
                target = target.queryParam(name, values);
            }
        }
        return target;
    }

    /**
     * Builds for an object.
     *
     * @param client a client
     * @param storageUrl storage URL
     * @param containerName container name
     * @param objectName object name
     * @param params query parameters
     * @param authToken authorization token
     * @return a builder
     */
    public static Invocation.Builder buildObject(
            final Client client, final String storageUrl,
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> params,
            final String authToken) {
        return targetObject(client, storageUrl, containerName, objectName,
                            params)
                .request()
                .header(HEADER_X_AUTH_TOKEN, authToken);
    }

    // ---------------------------------------------------------------- /account
//    /**
//     * Put {@link #HEADER_X_AUTH_ADMIN_USER} and
//     * {@link #HEADER_X_AUTH_ADMIN_KEY} on given invocation builder.
//     *
//     * @param invocationBuilder the invocation builder
//     * @param authUser the value for {@link #HEADER_X_AUTH_ADMIN_USER}
//     * @param authKey the value for {@link #HEADER_X_AUTH_ADMIN_KEY}
//     * @return given invocation builder
//     */
//    public static Invocation.Builder headersAuthAdminCredential(
//            final Invocation.Builder invocationBuilder, final String authUser,
//            final String authKey) {
//        return invocationBuilder
//                .header(HEADER_X_AUTH_ADMIN_USER, authUser)
//                .header(HEADER_X_AUTH_ADMIN_KEY, authKey);
//    }
    public static WebTarget targetAccount(
            final Client client, final String accountUrl,
            final MultivaluedMap<String, Object> params) {
        WebTarget target = client.target(accountUrl);
        if (params != null) {
            for (final Entry<String, List<Object>> entry : params.entrySet()) {
                final String name = entry.getKey();
                final Object[] values = entry.getValue().toArray();
                target = target.queryParam(name, values);
            }
        }
        return target;
    }

    public static Invocation.Builder buildAccount(
            final Client client, final String accountUrl,
            final MultivaluedMap<String, Object> params,
            final String authUser, final String authKey) {
        return targetAccount(client, accountUrl, params)
                .request()
                .header(HEADER_X_AUTH_ADMIN_USER, authUser)
                .header(HEADER_X_AUTH_ADMIN_KEY, authKey);
    }

    // ----------------------------------------------------------- /account/user
    public static WebTarget targetUser(
            final Client client, final String accountUrl,
            final String userName,
            final MultivaluedMap<String, Object> params) {
        return targetAccount(client, accountUrl, params).path(userName);
    }

    public static Invocation.Builder buildUser(
            final Client client, final String accountUrl,
            final String userName,
            final MultivaluedMap<String, Object> params,
            final String authUser, final String authKey) {
        return targetUser(client, accountUrl, userName, params)
                .request()
                .header(HEADER_X_AUTH_ADMIN_USER, authUser)
                .header(HEADER_X_AUTH_ADMIN_KEY, authKey);
    }

    // -------------------------------------------------------------------------
    public static void lines(final Response response, final Charset charset,
                             final Consumer<String> consumer) {
        try {
            try (InputStream stream = response.readEntity(InputStream.class)) {
                lines(stream, charset, consumer);
            }
        } catch (final IOException ioe) {
            throw new StorageClientException(ioe);
        }
    }

    public static StorageClientWsRs lines(
            final Response response, final Charset charset,
            final BiConsumer<String, StorageClientWsRs> consumer,
            final StorageClientWsRs client) {
        try {
            try (InputStream stream = response.readEntity(InputStream.class)) {
                return lines(stream, charset, consumer, client);
            }
        } catch (final IOException ioe) {
            throw new StorageClientException(ioe);
        }
    }

    public static void lines(final Response response,
                             final Consumer<String> consumer) {
        try {
            try (Reader reader = response.readEntity(Reader.class)) {
                lines(reader, consumer);
            }
        } catch (final IOException ioe) {
            throw new StorageClientException(ioe);
        }
    }

    public static StorageClientWsRs lines(
            final Response response,
            final BiConsumer<String, StorageClientWsRs> consumer,
            final StorageClientWsRs client) {
        try {
            try (Reader reader = response.readEntity(Reader.class)) {
                return lines(reader, consumer, client);
            }
        } catch (final IOException ioe) {
            throw new StorageClientException(ioe);
        }
    }

    // -------------------------------------------------------------------------
    /**
     * Creates a new instance.
     *
     * @param authUrl the authentication URL
     * @param authUser the authentication username
     * @param authKey the authentication password
     */
    public StorageClientWsRs(final String authUrl, final String authUser,
                             final String authKey) {
        super(authUrl, authUser, authKey);
    }

//    @Deprecated
//    public StorageClientWsRs(final StorageClient client) {
//        this(client.getStorageUrl(), client.getAuthUser(),
//             client.getAuthKey());
//        setStorageUrl(client.getStorageUrl());
//        setAuthToken(client.getAuthToken());
//        setAuthTokenExpires(client.getAuthTokenExpires());
//    }
    // -------------------------------------------------------------------------
    @Override
    public int getStatusCode(final Response response) {
        return response.getStatus();
    }
//
//    @Override
//    @Deprecated
//    protected String getReasonPhrase(final Response response) {
//        return response.getStatusInfo().getReasonPhrase();
//    }
//
//    @Override
//    protected String getHeaderValue(final Response response,
//                                    final String name) {
//        return response.getHeaderString(name);
//    }
    // -------------------------------------------------------------------------

    @Override
    public <R> R authenticateUser(final boolean newToken,
                                  final Function<Response, R> function) {
        return apply(c -> {
            final Response response = authenticateUser(
                    c, getAuthUrl(), getAuthUser(), getAuthKey(), newToken);
            try {
                if (OK.getStatusCode() != response.getStatus()) {
                    throw new WebApplicationException(
                            "failed to authenticate user", response);
                }
                setStorageUrl(response.getHeaderString(HEADER_X_STORAGE_URL));
                setAuthToken(response.getHeaderString(HEADER_X_AUTH_TOKEN));
                setAuthTokenExpires(response.getHeaderString(
                        HEADER_X_AUTH_TOKEN_EXPIRES));
                return function.apply(response);
            } finally {
                response.close();
            }
        });
    }

    // ---------------------------------------------------------------- /storage
    public <R> R peekStorage(final MultivaluedMap<String, Object> params,
                             final MultivaluedMap<String, Object> headers,
                             final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildStorage(
                    client, getStorageUrl(), params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.head();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R peekStorage(final Map<String, List<Object>> params,
                             final Map<String, List<Object>> headers,
                             final Function<Response, R> function) {
        return peekStorage(multivalued(params), multivalued(headers), function);
    }
//    public <R> R peekStorage(final MultivaluedMap<String, Object> params,
//                             final MultivaluedMap<String, Object> headers,
//                             final BiFunction<Response, StorageClientWsRs, R> function) {
//        final Client client = getClient();
//        try {
//            Invocation.Builder builder = buildStorage(
//                    client, getStorageUrl(), params, getAuthToken());
//            if (headers != null) {
//                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
//                builder = builder.headers(headers);
//            }
//            final Response response = builder.head();
//            try {
//                return function.apply(response, this);
//            } finally {
//                response.close();
//            }
//        } finally {
//            client.close();
//        }
//    }
//
//    @Override
//    public <R> R peekStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<Response, StorageClientWsRs, R> function) {
//        return peekStorage(multivalued(params), multivalued(headers), function);
//    }

    /**
     * Reads a storage using {@link javax.ws.rs.HttpMethod#GET}.
     *
     * @param <R> result type parameter
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value {@code function} results
     */
    public <R> R readStorage(final MultivaluedMap<String, Object> params,
                             final MultivaluedMap<String, Object> headers,
                             final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildStorage(
                    client, getStorageUrl(), params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readStorage(final Map<String, List<Object>> params,
                             final Map<String, List<Object>> headers,
                             final Function<Response, R> function) {
        return readStorage(multivalued(params), multivalued(headers), function);
    }

    public <R> R configureStorage(final MultivaluedMap<String, Object> params,
                                  final MultivaluedMap<String, Object> headers,
                                  final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildStorage(
                    client, getStorageUrl(), params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.post(null);
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R configureStorage(final Map<String, List<Object>> params,
                                  final Map<String, List<Object>> headers,
                                  final Function<Response, R> function) {
        return configureStorage(
                multivalued(params), multivalued(headers), function);
    }

    // ------------------------------------------------------ /account/container
    public <R> R peekContainer(final String containerName,
                               final MultivaluedMap<String, Object> params,
                               final MultivaluedMap<String, Object> headers,
                               final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildContainer(
                    client, getStorageUrl(), containerName, params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.head();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R peekContainer(final String containerName,
                               final Map<String, List<Object>> params,
                               final Map<String, List<Object>> headers,
                               final Function<Response, R> function) {
        return peekContainer(containerName, multivalued(params),
                             multivalued(headers), function);
    }

    public <R> R readContainer(final String containerName,
                               final MultivaluedMap<String, Object> params,
                               final MultivaluedMap<String, Object> headers,
                               final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildContainer(
                    client, getStorageUrl(), containerName, params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readContainer(final String containerName,
                               final Map<String, List<Object>> params,
                               final Map<String, List<Object>> headers,
                               final Function<Response, R> function) {
        return readContainer(containerName, multivalued(params),
                             multivalued(headers), function);
    }

    /**
     * Creates or updates a container.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function the function to be applied with the response; may be
     * {@code null}
     * @return the value the function results; {@code null} if the
     * {@code function} is {@code null}
     */
    public <R> R updateContainer(final String containerName,
                                 final MultivaluedMap<String, Object> params,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildContainer(
                    client, getStorageUrl(), containerName, params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder.headers(headers);
            }
            final Response response = builder.put(Entity.text(""));
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R updateContainer(final String containerName,
                                 final Map<String, List<Object>> params,
                                 final Map<String, List<Object>> headers,
                                 final Function<Response, R> function) {
        return updateContainer(containerName, multivalued(params),
                               multivalued(headers), function);
    }

    public <R> R configureContainer(
            final String containerName,
            final MultivaluedMap<String, Object> params,
            final MultivaluedMap<String, Object> headers,
            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildContainer(
                    client, getStorageUrl(), containerName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder.headers(headers);
            }
            final Response response = builder.post(null);
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R configureContainer(final String containerName,
                                    final Map<String, List<Object>> params,
                                    final Map<String, List<Object>> headers,
                                    final Function<Response, R> function) {
        return configureContainer(containerName, multivalued(params),
                                  multivalued(headers), function);
    }

    /**
     * Deletes a container identified by given name and returns the result .
     *
     * @param <R> return value type parameter
     * @param containerName the container name
     * @param params query parameters
     * @param headers additional request headers
     * @param function the function to be applied with the response.
     * @return the value function results or {@code null} if the
     * {@code function} is {@code null}.
     */
    public <R> R deleteContainer(final String containerName,
                                 final MultivaluedMap<String, Object> params,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildContainer(
                    client, getStorageUrl(), containerName, params, getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
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

    @Override
    public <R> R deleteContainer(final String containerName,
                                 final Map<String, List<Object>> params,
                                 final Map<String, List<Object>> headers,
                                 final Function<Response, R> function) {
        return deleteContainer(containerName, multivalued(params),
                               multivalued(headers), function);
    }

    // ----------------------------------------------- /account/container/object
    public <R> R peekObject(
            final String containerName, final String objectName,
            final MultivaluedMap<String, Object> params,
            final MultivaluedMap<String, Object> headers,
            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildObject(
                    client, getStorageUrl(), containerName, objectName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.head();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R peekObject(final String containerName, final String objectName,
                            final Map<String, List<Object>> params,
                            final Map<String, List<Object>> headers,
                            final Function<Response, R> function) {
        return peekObject(containerName, objectName, multivalued(params),
                          multivalued(headers), function);
    }

    public <R> R readObject(final String containerName, final String objectName,
                            final MultivaluedMap<String, Object> params,
                            final MultivaluedMap<String, Object> headers,
                            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildObject(
                    client, getStorageUrl(), containerName, objectName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readObject(final String containerName, final String objectName,
                            final Map<String, List<Object>> params,
                            final Map<String, List<Object>> headers,
                            final Function<Response, R> function) {
        return readObject(containerName, objectName, multivalued(params),
                          multivalued(headers), function);
    }

//    public <R> R updateObject(final String containerName,
//                              final String objectName,
//                              final MultivaluedMap<String, Object> params,
//                              final MultivaluedMap<String, Object> headers,
//                              final Supplier<Entity<?>> entity,
//                              final Function<Response, R> function) {
//        final Client client = getClient();
//        try {
//            Invocation.Builder builder = buildObject(
//                    client, getStorageUrl(), containerName, objectName, params,
//                    getAuthToken());
//            if (headers != null) {
//                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
//                builder = builder.headers(headers);
//            }
//            final Response response = builder.put(entity.get());
//            try {
//                return function.apply(response);
//            } finally {
//                response.close();
//            }
//        } finally {
//            client.close();
//        }
//    }
//
//    @Override
//    public <R> R updateObject(final String containerName,
//                              final String objectName,
//                              final Map<String, List<Object>> params,
//                              final Map<String, List<Object>> headers,
//                              final Supplier<Entity<?>> entity,
//                              final Function<Response, R> function) {
//        return updateObject(containerName, objectName, multivalued(params),
//                            multivalued(headers), entity, function);
//    }
    public <R> R updateObject(
            final String containerName,
            final String objectName,
            final MultivaluedMap<String, Object> params,
            final MultivaluedMap<String, Object> headers,
            final Function<Invocation.Builder, Response> function1,
            final Function<Response, R> function2) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildObject(
                    client, getStorageUrl(), containerName, objectName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = function1.apply(builder);
            try {
                return function2.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R updateObject(
            final String containerName, final String objectName,
            final Map<String, List<Object>> params,
            final Map<String, List<Object>> headers,
            final Function<Invocation.Builder, Response> function1,
            final Function<Response, R> function2) {
        return updateObject(containerName, objectName, multivalued(params),
                            multivalued(headers), function1, function2);
    }

    public <R> R configureObject(final String containerName,
                                 final String objectName,
                                 final MultivaluedMap<String, Object> params,
                                 final MultivaluedMap<String, Object> headers,
                                 final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildObject(
                    client, getStorageUrl(), containerName, objectName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
            }
            final Response response = builder.post(null);
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R configureObject(final String containerName,
                                 final String objectName,
                                 final Map<String, List<Object>> params,
                                 final Map<String, List<Object>> headers,
                                 final Function<Response, R> function) {
        return configureObject(containerName, objectName, multivalued(params),
                               multivalued(headers), function);
    }

    /**
     * Deletes an object using {@link javax.ws.rs.HttpMethod#DELETE}.
     *
     * @param <R> return value type parameter
     * @param containerName the container name
     * @param objectName the object name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}.
     * @param function a function to be applied with the server response; may be
     * {@code null}
     * @return a value the {@code function} results or {@code null} if the
     * {@code function} is {@code null}
     */
    public <R> R deleteObject(final String containerName,
                              final String objectName,
                              final MultivaluedMap<String, Object> params,
                              final MultivaluedMap<String, Object> headers,
                              final Function<Response, R> function) {
//        ensureValid();
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildObject(
                    client, getStorageUrl(), containerName, objectName, params,
                    getAuthToken());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_TOKEN, getAuthToken());
                builder = builder.headers(headers);
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

    @Override
    public <R> R deleteObject(final String containerName,
                              final String objectName,
                              final Map<String, List<Object>> params,
                              final Map<String, List<Object>> headers,
                              final Function<Response, R> function) {
        return deleteObject(containerName, objectName, multivalued(params),
                            multivalued(headers), function);
    }

    // ---------------------------------------------------------------- /account
    public <R> R readAccount(
            final MultivaluedMap<String, Object> params,
            final MultivaluedMap<String, Object> headers,
            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildAccount(
                    client, getAccountUrl(), params, getAuthUser(), getAuthKey());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_ADMIN_USER, getAuthUser());
                headers.putSingle(HEADER_X_AUTH_ADMIN_KEY, getAuthKey());
                builder.headers(headers);
            }
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readAccount(final Map<String, List<Object>> params,
                             final Map<String, List<Object>> headers,
                             final Function<Response, R> function) {
        return readAccount(multivalued(params), multivalued(headers),
                           function);
    }

    // ----------------------------------------------------------- /account/user
    public <R> R readUser(final String userName,
                          final MultivaluedMap<String, Object> params,
                          final MultivaluedMap<String, Object> headers,
                          final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildUser(
                    client, getAccountUrl(), userName, params, getAuthUser(), getAuthKey());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_ADMIN_USER, getAuthUser());
                headers.putSingle(HEADER_X_AUTH_ADMIN_KEY, getAuthKey());
                builder.headers(headers);
            }
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readUser(final String userName,
                          final Map<String, List<Object>> params,
                          final Map<String, List<Object>> headers,
                          final Function<Response, R> function) {
        return readUser(userName, multivalued(params), multivalued(headers),
                        function);
    }

    public <R> R updateUser(final String userName, final String userKey,
                            final boolean userAdmin,
                            final MultivaluedMap<String, Object> params,
                            MultivaluedMap<String, Object> headers,
                            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            Invocation.Builder builder = buildUser(
                    client, getAccountUrl(), userName, params, getAuthUser(), getAuthKey());
            if (headers == null) {
                headers = new MultivaluedHashMap<>();
            }
            headers.putSingle(HEADER_X_AUTH_ADMIN_USER, getAuthUser());
            headers.putSingle(HEADER_X_AUTH_ADMIN_KEY, getAuthKey());
            headers.putSingle(HEADER_X_AUTH_USER_KEY, userKey);
            if (userAdmin) {
                headers.putSingle(HEADER_X_AUTH_USER_ADMIN, userAdmin);
            }
            builder = builder.headers(headers);
            final Response response = builder.put(Entity.text(""));
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R updateUser(final String userName, final String userKey,
                            final boolean userAdmin,
                            final Map<String, List<Object>> params,
                            final Map<String, List<Object>> headers,
                            final Function<Response, R> function) {
        return updateUser(userName, userKey, userAdmin, multivalued(params),
                          multivalued(headers), function);
    }

    public <R> R deleteUser(final String userName,
                            final MultivaluedMap<String, Object> params,
                            final MultivaluedMap<String, Object> headers,
                            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final Invocation.Builder builder = buildUser(
                    client, getAccountUrl(), userName, params, getAuthUser(), getAuthKey());
            if (headers != null) {
                headers.putSingle(HEADER_X_AUTH_ADMIN_USER, getAuthUser());
                headers.putSingle(HEADER_X_AUTH_ADMIN_KEY, getAuthKey());
                builder.headers(headers);
            }
            final Response response = builder.delete();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();;
        }
    }

    @Override
    public <R> R deleteUser(final String userName,
                            final Map<String, List<Object>> params,
                            final Map<String, List<Object>> headers,
                            final Function<Response, R> function) {
        return deleteUser(userName, multivalued(params), multivalued(headers),
                          function);
    }

    // -------------------------------------------------------- /account/.groups
    public <R> R readGroups(final MultivaluedMap<String, Object> params,
                            final MultivaluedMap<String, Object> headers,
                            final Function<Response, R> function) {
        final Client client = getClient();
        try {
            final WebTarget target
                    = targetAccount(client, getAccountUrl(), params)
                    .path(".groups");
            final Invocation.Builder builder = target.request();
            if (headers != null) {
                builder.headers(headers);
            }
            authAdmin(builder, getAuthUser(), getAuthKey());
            final Response response = builder.get();
            try {
                return function.apply(response);
            } finally {
                response.close();
            }
        } finally {
            client.close();
        }
    }

    @Override
    public <R> R readGroups(final Map<String, List<Object>> params,
                            final Map<String, List<Object>> headers,
                            final Function<Response, R> function) {
        return readGroups(multivalued(params), multivalued(headers), function);
    }

    // ---------------------------------------------------------- clientSupplier
    public Supplier<Client> getClientSupplier() {
        return clientSupplier;
    }

    public void setClientSupplier(final Supplier<Client> clientSupplier) {
        this.clientSupplier = clientSupplier;
    }

    public StorageClientWsRs clientSupplier(
            final Supplier<Client> clientSupplier) {
        setClientSupplier(clientSupplier);
        return this;
    }

    // ------------------------------------------------------------------ client
    protected Client getClient() {
        return getClientSupplier().get();
    }

    protected <R> R apply(final Function<Client, R> function) {
        final Client client = getClient();
        try {
            return function.apply(client);
        } finally {
            client.close();
        }
    }

    protected <U, R> R apply(final BiFunction<Client, U, R> function,
                             final Supplier<U> supplier) {
        return apply(c -> function.apply(c, supplier.get()));
    }

    protected StorageClientWsRs accept(final Consumer<Client> consumer) {
        return apply(c -> {
            consumer.accept(c);
            return this;
        });
    }

    protected <U> StorageClientWsRs accept(final BiConsumer<Client, U> consumer,
                                           final Supplier<U> supplier) {
        return accept(c -> consumer.accept(c, supplier.get()));
    }

    // -------------------------------------------------------------------------
    private Supplier<Client> clientSupplier = () -> ClientBuilder.newClient();
}
