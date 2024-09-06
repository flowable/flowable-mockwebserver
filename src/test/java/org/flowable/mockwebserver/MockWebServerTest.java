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
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class MockWebServerTest {

    @Test
    void failFast() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.failFast();
            server.start();
            server.enqueue(MockResponse.newBuilder().build());
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("");
            assertThat(response.headers().map())
                    .containsOnly(
                            entry("content-length", Collections.singletonList("0"))
                    );

            response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(501);
            assertThat(response.body()).isEqualTo("");
            assertThat(response.headers().map())
                    .containsOnly(
                            entry("content-length", Collections.singletonList("0"))
                    );
        }
    }

    @Test
    void defaultResponse() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(MockResponse.newBuilder().build());
            server.defaultResponse(MockResponse.newBuilder().notFound().build());
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("");
            assertThat(response.headers().map())
                    .containsOnly(
                            entry("content-length", Collections.singletonList("0"))
                    );

            response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.body()).isEqualTo("");
            assertThat(response.headers().map())
                    .containsOnly(
                            entry("content-length", Collections.singletonList("0"))
                    );
        }
    }

    @Test
    void requestCount() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(501);

            assertThat(server.requestCount()).isEqualTo(1);

            response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(501);
            assertThat(server.requestCount()).isEqualTo(2);

            assertThat(server.takeRequest()).isNotNull();
            assertThat(server.takeRequest()).isNotNull();
            assertThat(server.takeRequest()).isNull();
        }
    }

    @Test
    void clearRequestsAndResponses() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(MockResponse.newBuilder().status(MockHttpStatus.OK));
            server.enqueue(MockResponse.newBuilder().status(MockHttpStatus.NO_CONTENT));
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);

            assertThat(server.requestCount()).isEqualTo(1);
            server.clearRequestsAndResponses();

            response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(501);
            assertThat(server.requestCount()).isEqualTo(2);

            assertThat(server.takeRequest()).isNotNull();
            assertThat(server.takeRequest()).isNull();
        }
    }

    @Test
    void startOnCustomPort() {
        try (MockWebServer server = new MockWebServer()) {
            server.start(50154);
            assertThat(server.getPort()).isEqualTo(50154);
        }
    }

    @Test
    void takeRequestWhenNoRequests() throws InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            long start = System.currentTimeMillis();
            RecordedRequest request = server.takeRequest();
            long end = System.currentTimeMillis();
            assertThat(request).isNull();
            assertThat(end - start).isLessThanOrEqualTo(100);

            start = System.currentTimeMillis();
            request = server.takeRequest(1, TimeUnit.SECONDS);
            end = System.currentTimeMillis();
            assertThat(request).isNull();
            assertThat(end - start).isGreaterThanOrEqualTo(1000);
        }
    }

    @Test
    void takeRequestWhenNoRequestsWithDuration() throws InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            long start = System.currentTimeMillis();
            RecordedRequest request = server.takeRequest();
            long end = System.currentTimeMillis();
            assertThat(request).isNull();
            assertThat(end - start).isLessThanOrEqualTo(100);

            start = System.currentTimeMillis();
            request = server.takeRequest(Duration.ofSeconds(1));
            end = System.currentTimeMillis();
            assertThat(request).isNull();
            assertThat(end - start).isGreaterThanOrEqualTo(1000);
        }
    }
}
