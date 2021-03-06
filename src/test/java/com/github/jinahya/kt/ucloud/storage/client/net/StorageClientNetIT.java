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
package com.github.jinahya.kt.ucloud.storage.client.net;

import static com.github.jinahya.kt.ucloud.storage.client.StorageClient.lines;
import com.github.jinahya.kt.ucloud.storage.client.StorageClientException;
import com.github.jinahya.kt.ucloud.storage.client.StorageClientIT;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import org.testng.SkipException;

/**
 *
 * @author Jin Kwon &lt;onacit at gmail.com&gt;
 */
public class StorageClientNetIT
        extends StorageClientIT<StorageClientNet, URLConnection, URLConnection> {

    private static final Logger logger
            = getLogger(MethodHandles.lookup().lookupClass());

    public StorageClientNetIT() {
        super(StorageClientNet.class);
    }

    @Override
    protected int assertSuccesfulAuthentication(final URLConnection response) {
        try {
            final int responseCode
                    = ((HttpURLConnection) response).getResponseCode();
            final String responseMessage
                    = ((HttpURLConnection) response).getResponseMessage();
            assertTrue(responseCode / 100 == 2,
                       "status: " + responseCode + " " + responseMessage);
            return responseCode;
        } catch (final IOException ioe) {
            fail("failed to get status info", ioe);
            throw new SkipException("failed to get status info", ioe);
        }
    }

    @Override
    protected int statusCode(final URLConnection response) {
        return StorageClientNet.statusCode((HttpURLConnection) response);
    }

    @Override
    protected String reasonPhrase(URLConnection response) {
        return StorageClientNet.reasonPhrase((HttpURLConnection) response);
    }

    @Override
    protected void printHeaders(URLConnection response) {
        response.getHeaderFields().forEach((k, vs) -> {
            vs.forEach(v -> {
                logger.debug("header: {}: {}", k, v);
            });
        });
    }

    @Override
    protected void printBody(URLConnection response) {
        try {
            try (InputStream stream = response.getInputStream()) {
                lines(stream,
                      UTF_8,
                      l -> {
                          logger.debug("line: {}", l);
                      }
                );
            }
        } catch (final IOException ioe) {
            fail("failed to print body", ioe);
        }
    }

//    @Override
//    protected InputStream requestEntity() {
//        final byte[] bytes
//                = new byte[ThreadLocalRandom.current().nextInt(1024)];
//        ThreadLocalRandom.current().nextBytes(bytes);
//        return new ByteArrayInputStream(bytes);
//    }
    @Override
    protected URLConnection requestEntity(final URLConnection connection) {
        final byte[] bytes
                = new byte[ThreadLocalRandom.current().nextInt(1024)];
        ThreadLocalRandom.current().nextBytes(bytes);
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
                output.flush();
            }
            return connection;
        } catch (final IOException ioe) {
            throw new StorageClientException(ioe);
        }
    }
}
