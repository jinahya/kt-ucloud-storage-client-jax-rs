/*
 * Copyright 2016 Jin Kwon &lt;onacit at gmail.com&gt;.
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
package com.github.jinahya.kt.ucloud.storage.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import static java.lang.System.currentTimeMillis;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;

/**
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 * @param <ClientType> storage client implementation type parameter
 * @param <RequestEntityType> request entity type parameter
 * @param <ResponseType> response type parameter
 */
public abstract class StorageClient<ClientType extends StorageClient, RequestEntityType, ResponseType> {

    private static final Logger logger
            = getLogger(StorageClient.class.getName());

    public static final String AUTH_URL_STANDARD_KOR_CENTER
            = "https://api.ucloudbiz.olleh.com/storage/v1/auth";

    public static final String AUTH_URL_STANDARD_JPN
            = "https://api.ucloudbiz.olleh.com/storage/v1/authjp";

    public static final String AUTH_URL_LITE_KOR_HA
            = "https://api.ucloudbiz.olleh.com/storage/v1/authlite";

    public static final String QUERY_PARAM_LIMIT = "limit";

    public static final String QUERY_PARAM_MARKER = "marker";

    public static final String QUERY_PARAM_FORMAT = "format";

//    public static final String HEADER_X_AUTH_USER = "X-Storage-User";
    public static final String HEADER_X_AUTH_USER = "X-Auth-User";

//    public static final String HEADER_X_AUTH_PASS = "X-Storage-Pass";
    public static final String HEADER_X_AUTH_KEY = "X-Auth-Key";

    public static final String HEADER_X_AUTH_NEW_TOKEN = "X-Auth-New-Token";

    public static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";

    public static final String HEADER_X_AUTH_TOKEN_EXPIRES
            = "X-Auth-Token-Expires";

    public static final String HEADER_X_STORAGE_URL = "X-Storage-Url";

    public static final String HEADER_X_ACCOUNT_OBJECT_COUNT
            = "X-Account-Object-Count";

    public static final String HEADER_X_ACCOUNT_BYTES_USED
            = "X-Account-Bytes-Used";

    public static final String HEADER_X_ACCOUNT_CONTAINER_COUNT
            = "X-Account-Container-Count";

    public static final String HEADER_X_CONTAINER_OBJECT_COUNT
            = "X-Container-Object-Count";

    public static final String HEADER_X_CONTAINER_BYTES_USED
            = "X-Container-Bytes-Used";

    public static final String HEADER_X_CONTAINER_READ = "X-Container-Read";

    public static final String HEADER_X_CONTAINER_WRITE = "X-Container-Write";

    public static final String HEADER_X_REMOVE_CONTAINER_READ
            = "X-Remove-Container-Read";

    public static final String HEADER_X_REMOVE_CONTAINER_WRITE
            = "X-Remove-Container-Write";

    public static final String HEADER_X_COPY_FROM = "X-Copy-From";

    public static final String HEADER_X_AUTH_ADMIN_USER = "X-Auth-Admin-User";

    public static final String HEADER_X_AUTH_ADMIN_KEY = "X-Auth-Admin-Key";

    public static final String HEADER_X_AUTH_USER_KEY = "X-Auth-User-Key";

    public static final String HEADER_X_AUTH_USER_ADMIN = "X-Auth-User-Admin";

    /**
     * Capitalizes given string. This method does nothing but returning the
     * string if the string is {@code null} or empty.
     *
     * @param token the string to capitalize
     * @return a capitalized value of given string
     */
    public static String capitalize(final String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        return token.substring(0, 1).toUpperCase()
               + token.substring(1).toLowerCase();
    }

    /**
     * Capitalizes each token and joins them with '{@code -}'. Note that each
     * token split by '{@code -}' first.
     *
     * @param tokens the tokens
     * @return a string
     */
    public static String capitalizeAndJoin(final String... tokens) {
        return stream(tokens)
                .flatMap(t -> Stream.of(t.split("-")))
                .map(v -> capitalize(v)).collect(joining("-"));
    }

    public static String metaHeader(final boolean remove, final String scope,
                                    final String... tokens) {
        return "X" + (remove ? "-Remove" : "") + "-" + scope + "-Meta" + "-"
               + capitalizeAndJoin(tokens);
    }

    public static String storageMetaHeader(final boolean remove,
                                           final String... tokens) {
        return metaHeader(remove, "Account", tokens);
    }

    public static String containerMetaHeader(final boolean remove,
                                             final String... tokens) {
        return metaHeader(remove, "Container", tokens);
    }

    public static String objectMetaHeader(final boolean remove,
                                          final String... tokens) {
        return metaHeader(remove, "Object", tokens);
    }

    /**
     * Creates a URL for an account from given storage URL and account name.
     *
     * @param storageUrl the storage URL
     * @param accountName the account name
     * @return a URL for an account
     */
    public static String accountUrl(final String storageUrl,
                                    final String accountName) {
        try {
            final URL url = new URL(storageUrl);
            final String protocol = url.getProtocol();
            final String authority = url.getAuthority();
            return protocol + "://" + authority + "/auth/v2/" + accountName;
        } catch (final MalformedURLException murle) {
            throw new StorageClientException(murle);
        }
    }

    /**
     * Accepts each lines of given {@code reader} to specified {@code consumer}.
     *
     * @param reader the reader
     * @param consumer the consumer
     */
    public static void lines(final Reader reader,
                             final Consumer<String> consumer) {
        new BufferedReader(reader).lines().forEach(consumer::accept);
    }

    /**
     * Accepts each lines of given input stream to specified consumer.
     *
     * @param stream the stream
     * @param charset a character set
     * @param consumer the consumer
     */
    public static void lines(final InputStream stream, final Charset charset,
                             final Consumer<String> consumer) {
        lines(new InputStreamReader(stream, charset), consumer);
    }

    /**
     * Accepts each lines of given {@code reader} and given {@code client} to
     * specified {@code consumer}.
     *
     * @param <ClientType> client type parameter
     * @param reader the reader
     * @param consumer the consumer
     * @param client the client
     * @return given {@code client}
     */
    public static <ClientType extends StorageClient> ClientType lines(
            final Reader reader, final BiConsumer<String, ClientType> consumer,
            final ClientType client) {
        lines(reader, l -> consumer.accept(l, client));
        return client;
    }

    public static <ClientType extends StorageClient> ClientType lines(
            final InputStream stream, final Charset charset,
            final BiConsumer<String, ClientType> consumer,
            final ClientType client) {
        return lines(new InputStreamReader(stream, charset), consumer, client);
    }

    // -------------------------------------------------------------------------
    /**
     * Creates a new instance.
     *
     * @param authUrl a URL for authentication
     * @param authUser username
     * @param authKey password
     */
    public StorageClient(final String authUrl, final String authUser,
                         final String authKey) {
        this.authUrl = requireNonNull(authUrl, "null authUrl");
        this.authUser = requireNonNull(authUser, "null authUser");
        {
            final int i = this.authUser.indexOf(':');
            accountName = i == -1 ? null : this.authUser.substring(0, i);
        }
        this.authKey = requireNonNull(authKey, "null authKey");
    }

    // -------------------------------------------------------------------------
    /**
     * Returns the value of {@code Status-Code} of given response.
     *
     * @param response the response
     * @return the value of {@code Status-Code} of given response
     */
    protected abstract int getStatusCode(ResponseType response);
//
//    @Deprecated
//    protected abstract String getReasonPhrase(ResponseType response);
//
//    /**
//     * Returns the header value of given response.
//     *
//     * @param response the response
//     * @param name the header name
//     * @return the value of header or {@code null} if no header found.
//     */
//    protected abstract String getHeaderValue(ResponseType response,
//                                             String name);
    // -------------------------------------------------------------------------

    public abstract <R> R authenticateUser(boolean newToken,
                                           Function<ResponseType, R> function);

//    public <R> R authenticateUser(
//            final boolean newToken,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return authenticateUser(
//                newToken,
//                response -> {
//                    return function.apply(response, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType authenticateUser(
//            final boolean newToken,
//            final Consumer<ResponseType> consumer) {
//        return authenticateUser(
//                newToken,
//                response -> {
//                    consumer.accept(response);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType authenticateUser(
//            final boolean newToken,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return authenticateUser(
//                newToken,
//                response -> {
//                    consumer.accept(response, (ClientType) this);
//                }
//        );
//    }
    /**
     * Purges authentication information.
     *
     * @return this client
     */
    public ClientType invalidate() {
        storageUrl = null;
        authToken = null;
        authTokenExpires = null;
        return (ClientType) this;
    }

    /**
     * Checks if the authorization information is valid until given
     * milliseconds.
     *
     * @param until the milliseconds
     * @return {@code true} if the token is value until given milliseconds,
     * {@code false} otherwise.
     */
    public boolean isValid(final long until) {
        return storageUrl != null && authToken != null
               && authTokenExpires != null
               && authTokenExpires.getTime() >= until;
    }

    /**
     * Checks if the authentication token is valid in specified unit and
     * duration.
     *
     * @param unit the unit of time
     * @param duration the duration of the time unit.
     * @return {@code true} if the token is valid until specified time;
     * {@code false} otherwise.
     */
    public boolean isValid(final TimeUnit unit, final long duration) {
        return isValid(currentTimeMillis() + unit.toMillis(duration));
    }

    // ---------------------------------------------------------------- /storage
    /**
     * Peeks the storage using the {@code HEAD} method. Note that the
     * {@code Accept} header (with any value; e.g. *\/*} might be required.
     *
     * @param <R> result type parameter
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R peekStorage(Map<String, List<Object>> params,
                                      Map<String, List<Object>> headers,
                                      Function<ResponseType, R> function);
//    public <R> R peekStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return peekStorage(
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//    public abstract <R> R peekStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function);

//    public ClientType peekStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return peekStorage(
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//    public ClientType peekStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return peekStorage(
//                params,
//                headers,
//                (response, client) -> {
//                    consumer.accept(response, client);
//                    return client;
//                }
//        );
//    }
    /**
     * Reads the storage using {@code GET} method.
     *
     * @param <R> result type parameter
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R readStorage(final Map<String, List<Object>> params,
                                      final Map<String, List<Object>> headers,
                                      final Function<ResponseType, R> function);

//    public <R> R readStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readStorage(
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readStorage(
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readStorage(
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    /**
     * Reads container names and accepts each of them to given consumer. Put
     * parameters such as {@link #QUERY_PARAM_LIMIT} or
     * {@link #QUERY_PARAM_MARKER} into {@code params} if required.
     *
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function for yielding a {@code Reader} from the server
     * response. Make the function to throw a {@link StorageClientException} to
     * stop the iteration.
     * @param consumer a consumer accepts each container names
     * @return this client
     */
    public ClientType readStorageContainerNames(
            Map<String, List<Object>> params,
            Map<String, List<Object>> headers,
            final Function<ResponseType, Reader> function,
            final Consumer<String> consumer) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(QUERY_PARAM_LIMIT, singletonList(512));
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Accept", singletonList("text/plain"));
        final String marker_;
        {
            String marker__;
            try {
                marker__ = params.get(QUERY_PARAM_MARKER).get(0).toString();
            } catch (NullPointerException | IndexOutOfBoundsException e) {
                marker__ = null;
            }
            marker_ = marker__;
        }
        for (final String[] marker = new String[]{marker_};;) {
            if (marker[0] != null) {
                params.put(QUERY_PARAM_MARKER, singletonList(marker[0]));
            }
            marker[0] = null;
            final Object result = readStorage(
                    params,
                    headers,
                    r -> {
                        try {
                            try {
                                try (Reader reader = function.apply(r)) {
                                    lines(reader, consumer);
                                }
                                return null;
                            } catch (final StorageClientException sce) {
                                return sce;
                            }
                        } catch (final IOException ioe) {
                            throw new StorageClientException(ioe);
                        }
                    }
            );
            if (result != null) {
                break;
            }
            if (marker[0] == null) {
                break;
            }
        }
        return (ClientType) this;
    }

//    public ClientType readStorageContainerNames(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Function<ResponseType, Reader> function,
//            final BiConsumer<String, ClientType> consumer) {
//        return readStorageContainerNames(
//                params,
//                headers,
//                function,
//                l -> consumer.accept(l, (ClientType) this)
//        );
//    }
    public abstract <R> R configureStorage(Map<String, List<Object>> params,
                                           Map<String, List<Object>> headers,
                                           Function<ResponseType, R> function);

//    public <R> R configureStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return configureStorage(
//                params,
//                headers,
//                response -> {
//                    return function.apply(response, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType configureStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return configureStorage(
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType configureStorage(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return configureStorage(
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    // ------------------------------------------------------ /storage/container
    /**
     * Peeks a container using {@code HEAD} method.
     *
     * @param <R> result type parameter
     * @param containerName the name of the container
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R peekContainer(String containerName,
                                        Map<String, List<Object>> params,
                                        Map<String, List<Object>> headers,
                                        Function<ResponseType, R> function);

//    public <R> R peekContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return peekContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType peekContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return peekContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType peekContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return peekContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    /**
     * Reads a container using {@code GET} method.
     *
     * @param <R> result type parameter
     * @param containerName the name of the container
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code funtion} results
     */
    public abstract <R> R readContainer(String containerName,
                                        Map<String, List<Object>> params,
                                        Map<String, List<Object>> headers,
                                        Function<ResponseType, R> function);

//    public <R> R readContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readContainer(
//                containerName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    /**
     * Reads object names in a container and accepts each of them to specified
     * consumer.
     *
     * @param containerName the name of the container
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function for creating a reader from the server
     * response; make this function to throw a {@link StorageClientException}
     * for stopping the iteration.
     * @param consumer the consumer
     * @return this client.
     */
    public ClientType readContainerObjectNames(
            final String containerName, Map<String, List<Object>> params,
            Map<String, List<Object>> headers,
            final Function<ResponseType, Reader> function,
            final Consumer<String> consumer) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.putIfAbsent(QUERY_PARAM_LIMIT, singletonList(512));
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("Accept", singletonList("text/plain"));
        final String marker_;
        {
            String marker__;
            try {
                marker__ = params.get(QUERY_PARAM_MARKER).get(0).toString();
            } catch (NullPointerException | IndexOutOfBoundsException e) {
                marker__ = null;
            }
            marker_ = marker__;
        }
        for (final String[] marker = new String[]{marker_};;) {
            if (marker[0] != null) {
                params.put(QUERY_PARAM_MARKER, singletonList(marker[0]));
            }
            marker[0] = null;
            final Object result = readContainer(
                    containerName,
                    params,
                    headers,
                    r -> {
                        try {
                            try {
                                try (Reader reader = function.apply(r)) {
                                    lines(reader, consumer);
                                }
                                return null;
                            } catch (final StorageClientException sce) {
                                return sce;
                            }
                        } catch (final IOException ioe) {
                            throw new StorageClientException(ioe);
                        }
                    }
            );
            if (result != null) {
                break;
            }
            if (marker[0] == null) {
                break;
            }
        }
        return (ClientType) this;
    }

//    public ClientType readContainerObjectNames(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Function<ResponseType, Reader> function,
//            final BiConsumer<String, ClientType> consumer) {
//        return readContainerObjectNames(
//                containerName,
//                params,
//                headers,
//                function,
//                l -> consumer.accept(l, (ClientType) this)
//        );
//    }
    /**
     * Creates or updates a container using {@code PUT} method.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R updateContainer(String containerName,
                                          Map<String, List<Object>> params,
                                          Map<String, List<Object>> headers,
                                          Function<ResponseType, R> function);

//    public <R> R updateContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return updateContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType updateContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return updateContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType updateContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return updateContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    /**
     * Configures a container using the {@code POST} method.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R configureContainer(
            final String containerName,
            final Map<String, List<Object>> params,
            final Map<String, List<Object>> headers,
            final Function<ResponseType, R> function);

//    public <R> R configureContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return configureContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType configureContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return configureContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType configureContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return configureContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    /**
     * Deletes a container using {@code DELETE} method.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R deleteContainer(String containerName,
                                          Map<String, List<Object>> params,
                                          Map<String, List<Object>> headers,
                                          Function<ResponseType, R> function);

    /**
     * Deletes a container.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param function a function to be applied with the server response
     * @return the result the {@code function} results
     */
    public <R> R deleteContainer(final String containerName,
                                 final Function<ResponseType, R> function) {
        return deleteContainer(containerName, null, null, function);
    }

    /**
     * Deletes a container and returns the status code of the server response.
     *
     * @param containerName container name
     * @return the status code of the server response
     */
    public int deleteContainer(final String containerName) {
        return deleteContainer(containerName, this::getStatusCode);
    }

//    public <R> R deleteContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return deleteContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType deleteContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return deleteContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType deleteContainer(
//            final String containerName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return deleteContainer(
//                containerName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    // ----------------------------------------------- /storage/container/object
    /**
     * Peeks an object using {@code HEAD} method.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param objectName object name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response.
     * @return the result the {@code function} results
     */
    public abstract <R> R peekObject(String containerName, String objectName,
                                     Map<String, List<Object>> params,
                                     Map<String, List<Object>> headers,
                                     Function<ResponseType, R> function);

//    public <R> R peekObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return peekObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType peekObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return peekObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType peekObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return peekObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    public abstract <R> R readObject(String containerName, String objectName,
                                     Map<String, List<Object>> params,
                                     Map<String, List<Object>> headers,
                                     Function<ResponseType, R> function);

//    public <R> R readObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
    // ---------------------------------------- /storage/container/object/update
    public abstract <R> R updateObject(String containerName, String objectName,
                                       Map<String, List<Object>> params,
                                       Map<String, List<Object>> headers,
                                       Supplier<RequestEntityType> supplier,
                                       Function<ResponseType, R> function);

//    public <R> R updateObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Supplier<RequestEntityType> entity,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return updateObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                entity,
//                n -> {
//                    return function.apply(n, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType updateObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Supplier<RequestEntityType> supplier,
//            final Consumer<ResponseType> consumer) {
//        return updateObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                supplier,
//                n -> {
//                    consumer.accept(n);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType updateObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Supplier<RequestEntityType> supplier,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return updateObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                supplier,
//                n -> {
//                    consumer.accept(n, (ClientType) this);
//                }
//        );
//    }
//    public ClientType copyObject(
//            final String targetContainerName, final String targetObjectName,
//            final String sourceContainerName, final String sourceObjectName,
//            final BiConsumer<ResponseType, ResponseType> consumer) {
//        final Map<String, List<Object>> headers = new HashMap<>();
//        headers.put("Accept", singletonList("*/*"));
//        return peekObject(sourceContainerName,
//                          sourceObjectName,
//                          null,
//                          headers,
//                          (r1, c) -> {
//                              final int statusCode = getStatusCode(r1);
//                              if (statusCode != 204) {
//                                  logger.warning("peeking object failed with status code: " + statusCode);
//                                  consumer.accept(r1, null);
//                                  return (ClientType) this;
//                              }
//                              final String contentLength = getHeaderValue(r1, "content-length");
//                              if (contentLength == null) {
//                                  logger.warning("peeking object failed with absence of content-length");
//                                  consumer.accept(r1, null);
//                                  return (ClientType) this;
//                              }
//                              c.updateObject
//                              return (ClientType) this;
//                          }
//        );
//    }
    // ------------------------------------- /storage/container/object/configure
    public abstract <R> R configureObject(String containerName,
                                          String objectName,
                                          Map<String, List<Object>> params,
                                          Map<String, List<Object>> headers,
                                          Function<ResponseType, R> function);

//    public <R> R configureObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return configureObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType configureObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return configureObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType configureObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return configureObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    /**
     * Deletes an object using {@code DELETE} method.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param objectName object name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response.
     * @return the value the {@code function} results
     */
    public abstract <R> R deleteObject(String containerName,
                                       String objectName,
                                       Map<String, List<Object>> params,
                                       Map<String, List<Object>> headers,
                                       Function<ResponseType, R> function);

    /**
     * Deletes an object without any query parameters and request headers.
     *
     * @param <R> result type parameter
     * @param containerName container name
     * @param objectName object name
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public <R> R deleteObject(final String containerName,
                              final String objectName,
                              final Function<ResponseType, R> function) {
        return deleteObject(containerName, objectName, null, null, function);
    }

    /**
     * Deletes an object and returns the status code of the server response.
     *
     * @param containerName container name
     * @param objectName object name
     * @return the status code of the server response
     */
    public int deleteObject(final String containerName,
                            final String objectName) {
        return deleteObject(containerName, objectName, this::getStatusCode);
    }

//    public <R> R deleteObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return deleteObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType deleteObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return deleteObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType deleteObject(
//            final String containerName, final String objectName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return deleteObject(
//                containerName,
//                objectName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    // ---------------------------------------------------------------- /account
    /**
     * Reads account information.
     *
     * @param <R> result type parameter
     * @param params query parameters; may be {@code null}
     * @param headers additional request headers; may be {@code null}
     * @param function the function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R readAccount(
            Map<String, List<Object>> params, Map<String, List<Object>> headers,
            Function<ResponseType, R> function);

    /**
     * Reads account information without any query parameters and request
     * headers.
     *
     * @param <R> result type parameter
     * @param function the function to be applied with the server response
     * @return the value the {@code function} results
     * @see #readAccount(java.util.Map, java.util.Map,
     * java.util.function.Function)
     */
    public <R> R readAccount(final Function<ResponseType, R> function) {
        return readAccount(null, null, function);
    }

//    public <R> R readAccount(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readAccount(
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readAccount(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readAccount(
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readAccount(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readAccount(
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    // ----------------------------------------------------------- /account/user
    /**
     * Reads user information.
     *
     * @param <R> result type parameter
     * @param userName username
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function the function to be applied with server response
     * @return the value the {@code function} results
     */
    public abstract <R> R readUser(String userName,
                                   Map<String, List<Object>> params,
                                   Map<String, List<Object>> headers,
                                   Function<ResponseType, R> function);

    /**
     * Reads user information with any query parameter and additional request
     * headers.
     *
     * @param <R> result type parameter
     * @param userName user name
     * @param function the function to be applied with the server response
     * @return the value the {@code funtion} results
     * @see #readUser(java.lang.String, java.util.Map, java.util.Map,
     * java.util.function.Function)
     */
    public <R> R readUser(final String userName,
                          final Function<ResponseType, R> function) {
        return readUser(userName, null, null, function);
    }

//    public <R> R readUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    public abstract <R> R updateUser(
            final String userName, final String userKey,
            final boolean userAdmin, final Map<String, List<Object>> params,
            final Map<String, List<Object>> headers,
            final Function<ResponseType, R> function);

//    public <R> R updateUser(
//            final String userName, final String userKey,
//            final boolean userAdmin, final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return updateUser(
//                userName,
//                userKey,
//                userAdmin,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType updateUser(
//            final String userName, final String userKey,
//            final boolean userAdmin, final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return updateUser(
//                userName,
//                userKey,
//                userAdmin,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType updateUser(
//            final String userName, final String userKey,
//            final boolean userAdmin, final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return updateUser(
//                userName,
//                userKey,
//                userAdmin,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    /**
     * Deletes a user using the {@code DELETE} method.
     *
     * @param <R> result type parameter
     * @param userName user name
     * @param params query parameters; may be {@code null}
     * @param headers request headers; may be {@code null}
     * @param function a function to be applied with the server response
     * @return the value the {@code function} results
     */
    public abstract <R> R deleteUser(
            String userName, Map<String, List<Object>> params,
            Map<String, List<Object>> headers,
            Function<ResponseType, R> function);

    /**
     * Deletes a user without any query parameters and request headers.
     *
     * @param <R> result type parameter
     * @param userName user name
     * @param function a function to be applied with the server response
     * @return the result the {@code function} results
     */
    public <R> R deleteUser(final String userName,
                            final Function<ResponseType, R> function) {
        return deleteUser(userName, null, null, function);
    }

    /**
     * Deletes a user and returns the status code of the server response.
     *
     * @param userName user name
     * @return the status code of the server response
     */
    public int deleteUser(final String userName) {
        return deleteUser(userName, this::getStatusCode);
    }

//    public <R> R deleteUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return deleteUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType deleteUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return deleteUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType deleteUser(
//            final String userName,
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return deleteUser(
//                userName,
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    // -------------------------------------------------------- /account/.groups
    public abstract <R> R readGroups(Map<String, List<Object>> params,
                                     Map<String, List<Object>> headers,
                                     Function<ResponseType, R> function);

//    public <R> R readGroups(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiFunction<ResponseType, ClientType, R> function) {
//        return readGroups(
//                params,
//                headers,
//                r -> {
//                    return function.apply(r, (ClientType) this);
//                }
//        );
//    }
//
//    public ClientType readGroups(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final Consumer<ResponseType> consumer) {
//        return readGroups(
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r);
//                    return (ClientType) this;
//                }
//        );
//    }
//
//    public ClientType readGroups(
//            final Map<String, List<Object>> params,
//            final Map<String, List<Object>> headers,
//            final BiConsumer<ResponseType, ClientType> consumer) {
//        return readGroups(
//                params,
//                headers,
//                r -> {
//                    consumer.accept(r, (ClientType) this);
//                }
//        );
//    }
    // ----------------------------------------------------------------- authUrl
    /**
     * Returns the {@code authUrl}.
     *
     * @return the {@code authUrl}.
     * @deprecated forRemoval = true
     */
    @Deprecated//(forRemoval = true)
    public String getAuthUrl() {
        return authUrl;
    }

    // ---------------------------------------------------------------- authUser
    /**
     * Returns the {@code authUser}.
     *
     * @return the {@code authUser}.
     * @deprecated forRemoval = true
     */
    @Deprecated//(forRemoval = true)
    public String getAuthUser() {
        return authUser;
    }

    // ----------------------------------------------------------------- authKey
    /**
     * Returns the {@code authKey}.
     *
     * @return the {@code authKey}
     * @deprecated forRemoval = true
     */
    @Deprecated//(forRemoval = true)
    public String getAuthKey() {
        return authKey;
    }

    // ------------------------------------------------------------- accountName
    public String getAccountName() {
        return accountName;
    }

    public String accountName() {
        return getAccountName();
    }

    // -------------------------------------------------------------- storageUrl
    /**
     * Returns the storage URL.
     *
     * @return the storage URL
     */
    protected String getStorageUrl() {
        return storageUrl;
    }

    protected String storageUrl() {
        return getStorageUrl();
    }

    protected void setStorageUrl(final String storageUrl) {
        this.storageUrl = requireNonNull(storageUrl, "null storageUrl");
        if (accountName != null) {
            accountUrl = accountUrl(storageUrl, accountName);
        }
    }

    protected StorageClient storegeUrl(final String storageUrl) {
        setStorageUrl(storageUrl);
        return this;
    }

    // -------------------------------------------------------------- accountUrl
    protected String getAccountUrl() {
        return accountUrl;
    }

    protected String accountUrl() {
        return getAccountUrl();
    }

    // --------------------------------------------------------------- authToken
    /**
     * Returns the authorization token.
     *
     * @return the authorization token.
     */
    protected String getAuthToken() {
        return authToken;
    }

    protected void setAuthToken(final String authToken) {
        this.authToken = authToken;
    }

    // ------------------------------------------------------------ tokenExpires
    /**
     * Return the date the authorization token expires.
     *
     * @return the date the authorization token expires.
     */
    public Date getAuthTokenExpires() {
        if (authTokenExpires == null) {
            return null;
        }
        return new Date(authTokenExpires.getTime());
    }

    protected void setAuthTokenExpires(final Date authTokenExpires) {
        this.authTokenExpires
                = ofNullable(authTokenExpires)
                .map(Date::getTime)
                .map(Date::new)
                .orElse(null);
    }

    protected void setAuthTokenExpires(final String authTokenExpires) {
        setAuthTokenExpires(
                ofNullable(authTokenExpires)
                .map(Integer::parseInt)
                .map(SECONDS::toMillis)
                .map(v -> new Date(currentTimeMillis() + v))
                .orElse(null)
        );
    }

    // -------------------------------------------------------------------------
    protected final String authUrl;

    protected final String authUser;

    protected final String authKey;

    private final String accountName;

    private transient String storageUrl;

    private transient String accountUrl;

    private transient String authToken;

    private transient Date authTokenExpires;
}
