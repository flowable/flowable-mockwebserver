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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Filip Hrisafov
 */
class MockResponseBuilderTest {

    protected MockWebServer server = new MockWebServer();
    protected HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @EnumSource
    @ParameterizedTest
    void status(MockHttpStatus status) throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().status(status).build());

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(status.code());
    }

    @EnumSource
    @ParameterizedTest
    void statusCode(MockHttpStatus status) throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().statusCode(status.code()).build());

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(status.code());
    }

    @Test
    void customStatusCode() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().statusCode(228));

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(228);
    }

    @Test
    void customStatusAndReason() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().status(200, "Test"));

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void customHeaders() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder()
                .header("Content-Type", "application/json")
                .header("Content-Type", "text/plain")
                .header("X-Custom-Header", "custom-value")
                .addHeader("X-Custom-Header", "custom-value2")
                .build());

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.headers().map())
                .containsOnly(
                        entry("content-type", Collections.singletonList("text/plain")),
                        entry("content-length", Collections.singletonList("0")),
                        entry("x-custom-header", List.of("custom-value", "custom-value2"))
                );
    }

    @Test
    void invalidStatus() {
        assertThatThrownBy(() -> MockResponse.newBuilder().status(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status cannot be null");
    }

    @Test
    void bodyFromInputStream() throws IOException, InterruptedException {
        try (InputStream stream = new ByteArrayInputStream("Test stream body".getBytes(StandardCharsets.UTF_8))) {
            server.enqueue(MockResponse.newBuilder()
                    .body(stream));
        }

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo("Test stream body");
        assertThat(response.headers().map())
                .containsOnly(
                        entry("content-length", Collections.singletonList("16"))
                );
    }

    @Test
    void bodyFromBytes() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder()
                .body("Test bytes body".getBytes(StandardCharsets.UTF_8)));

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo("Test bytes body");
        assertThat(response.headers().map())
                .containsOnly(
                        entry("content-length", Collections.singletonList("15"))
                );
    }

    @Test
    void bodyFromString() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder()
                .body("Test string body"));

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo("Test string body");
        assertThat(response.headers().map())
                .containsOnly(
                        entry("content-length", Collections.singletonList("16"))
                );
    }

    @Test
    void jsonBody() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder()
                .jsonBody("""
                        {
                          "key": "value"
                        }
                        """));

        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.body()).isEqualTo("""
                {
                  "key": "value"
                }
                """);
        assertThat(response.headers().map())
                .containsOnly(
                        entry("content-type", Collections.singletonList("application/json")),
                        entry("content-length", Collections.singletonList("21"))
                );
    }

    @Test
    void bodyDelayWithTimeout() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().body("Test body").bodyDelay(1000, TimeUnit.MILLISECONDS));

        long start = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());
        long end = System.currentTimeMillis();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(end - start).isGreaterThanOrEqualTo(1000);
    }

    @Test
    void bodyDelayWithInvalidTimeout() {
        assertThatThrownBy(() -> MockResponse.newBuilder().bodyDelay(0, TimeUnit.MILLISECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("delay must be greater than 0");
        assertThatThrownBy(() -> MockResponse.newBuilder().bodyDelay(-10, TimeUnit.MILLISECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("delay must be greater than 0");
    }

    @Test
    void bodyDelayWithDuration() throws IOException, InterruptedException {
        server.enqueue(MockResponse.newBuilder().body("Test body").bodyDelay(Duration.ofSeconds(1)));

        long start = System.currentTimeMillis();
        HttpResponse<String> response = httpClient.send(createRequest(), HttpResponse.BodyHandlers.ofString());
        long end = System.currentTimeMillis();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(end - start).isGreaterThanOrEqualTo(1000);
    }

    @Test
    void bodyDelayWithInvalidDuration() {
        assertThatThrownBy(() -> MockResponse.newBuilder().bodyDelay(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("delay must be positive");
        assertThatThrownBy(() -> MockResponse.newBuilder().bodyDelay(Duration.ofSeconds(-10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("delay must be positive");
    }

    protected HttpRequest createRequest() {
        return HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(server.url()))
                .build();
    }
}
