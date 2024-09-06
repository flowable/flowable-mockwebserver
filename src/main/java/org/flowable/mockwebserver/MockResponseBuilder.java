/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.mockwebserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.microhttp.Header;
import org.microhttp.Response;

/**
 * A builder for creating {@link MockResponse} instances.
 *
 * @author Filip Hrisafov
 */
public final class MockResponseBuilder {

    private static final byte[] EMPTY_BYTE = new byte[0];

    private MockHttpStatusCode status = MockHttpStatus.OK;
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private byte[] body = EMPTY_BYTE;
    private Duration delay;

    private MockResponseBuilder() {
        header("Content-Length", "0");
    }

    /**
     * Create a new builder for a {@link MockResponse} with a default status of {@link MockHttpStatus#OK}.
     *
     * @return The builder for fluent API
     */
    public static MockResponseBuilder newBuilder() {
        return new MockResponseBuilder();
    }

    /**
     * Set the status of the response to {@link MockHttpStatus#OK}.
     *
     * @return The builder for fluent API
     */
    public MockResponseBuilder ok() {
        return status(MockHttpStatus.OK);
    }

    /**
     * Set the status of the response to {@link MockHttpStatus#NOT_FOUND}.
     *
     * @return The builder for fluent API
     */
    public MockResponseBuilder notFound() {
        return status(MockHttpStatus.NOT_FOUND);
    }

    /**
     * Set the status of the response to the given code.
     *
     * @param code the status code of the response
     * @return The builder for fluent API
     */
    public MockResponseBuilder statusCode(int code) {
        return status(MockHttpStatusCode.from(code));
    }

    /**
     * Set the status of the response to the given code and reason.
     *
     * @param code the status code of the response
     * @param reason the reason of the response
     * @return The builder for fluent API
     */
    public MockResponseBuilder status(int code, String reason) {
        return status(MockHttpStatusCode.from(code, reason));
    }

    /**
     * Set the status of the response.
     *
     * @param status the status of the response
     *
     * @return The builder for fluent API
     */
    public MockResponseBuilder status(MockHttpStatusCode status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        this.status = status;
        return this;
    }

    /**
     * Set a header on the response.
     * This will remove any existing headers with the same name.
     *
     * @param name the name of the header
     * @param value the value of the header
     * @return The builder for fluent API
     */
    public MockResponseBuilder header(String name, String value) {
        headers.remove(name);
        return addHeader(name, value);
    }

    /**
     * Add a header to the response.
     *
     * @param name the name of the header
     * @param value the value of the header
     * @return The builder for fluent API
     */
    public MockResponseBuilder addHeader(String name, String value) {
        this.headers.computeIfAbsent(name, k -> new ArrayList<>())
                .add(value);
        return this;
    }

    /**
     * Set the body of the response to the bytes read from the input stream.
     * This method will read all the bytes from the body.
     *
     * @param body the input stream to read the bytes from
     * @return The builder for fluent API
     */
    public MockResponseBuilder body(InputStream body) {
        try {
            return body(body.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read bytes", e);
        }
    }

    /**
     * Set the body of the response to the bytes.
     * This method will read all the bytes from the body.
     *
     * @param body the input stream to read the bytes from
     * @return The builder for fluent API
     */
    public MockResponseBuilder body(byte[] body) {
        this.body = Arrays.copyOf(body, body.length);
        header("Content-Length", String.valueOf(this.body.length));
        return this;
    }

    /**
     * Set the body of the response to the given string.
     * And set the content type to {@code application/json}
     * The charset used is {@link StandardCharsets#UTF_8}.
     *
     * @param body the body of the response
     * @return The builder for fluent API
     */
    public MockResponseBuilder jsonBody(String body) {
        return jsonBody(body, StandardCharsets.UTF_8);
    }

    /**
     * Set the body of the response to the given string.
     * And set the content type to {@code application/json}
     *
     * @param body the body of the response
     * @param charset the charset of the body
     * @return The builder for fluent API
     */
    public MockResponseBuilder jsonBody(String body, Charset charset) {
        body(body, charset);
        return header("Content-Type", "application/json");
    }

    /**
     * Set the body of the response to the given string.
     * The charset used is {@link StandardCharsets#UTF_8}.
     *
     * @param body the body of the response
     * @return The builder for fluent API
     */
    public MockResponseBuilder body(String body) {
        return body(body, StandardCharsets.UTF_8);
    }

    /**
     * Set the body of the response to the given string.
     *
     * @param body the body of the response
     * @param charset the charset of the body
     * @return The builder for fluent API
     */
    public MockResponseBuilder body(String body, Charset charset) {
        this.body = body.getBytes(charset);
        header("Content-Length", String.valueOf(this.body.length));
        return this;
    }

    /**
     * Set the delay of the response body.
     * The delay needs to be greater than 0.
     *
     * @param delay the delay of the response body
     * @param timeUnit the time unit of the delay
     * @return The builder for fluent API
     */
    public MockResponseBuilder bodyDelay(long delay, TimeUnit timeUnit) {
        if (delay <= 0) {
            throw new IllegalArgumentException("delay must be greater than 0");
        }
        return bodyDelay(Duration.ofMillis(TimeUnit.MILLISECONDS.convert(delay, timeUnit)));
    }

    /**
     * Set the delay of the response body.
     * The delay must be positive.
     *
     * @param delay the delay of the response body
     * @return The builder for fluent API
     */
    public MockResponseBuilder bodyDelay(Duration delay) {
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("delay must be positive");
        }
        this.delay = delay;
        return this;
    }

    /**
     * Build the {@link MockResponse} instance.
     *
     * @return the {@link MockResponse} instance
     */
    public MockResponse build() {
        List<Header> headers = new ArrayList<>(this.headers.size());
        for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
            String headerName = entry.getKey();
            for (String value : entry.getValue()) {
                headers.add(new Header(headerName, value));
            }
        }

        Response response = new Response(status.code(), status.reason(), headers, body);
        return new MockResponse(response, delay);
    }

}
