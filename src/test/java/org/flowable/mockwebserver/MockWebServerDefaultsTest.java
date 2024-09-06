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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class MockWebServerDefaultsTest {

    @Test
    void defaultResponseWhenNothingSet() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(server.url()))
                    .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(501);
            assertThat(response.body()).isEqualTo("No response has been configured for the Mock Web Server");
            assertThat(response.headers().map())
                    .containsOnly(
                            entry("content-length", Collections.singletonList("55")),
                            entry("content-type", Collections.singletonList("text/plain"))
                    );
        }
    }

    @Test
    void enqueueNonConfiguredMockResponse() throws IOException, InterruptedException {
        try (MockWebServer server = new MockWebServer()) {
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
        }
    }


    @Test
    void urlPriorStart() {
        try (MockWebServer server = new MockWebServer()) {
            assertThatThrownBy(server::url)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Server not started");
        }
    }

    @Test
    void urlWithPathPriorStart() {
        try (MockWebServer server = new MockWebServer()) {
            assertThatThrownBy(() -> server.url("/customers"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Server not started");
        }
    }

    @Test
    void urlAfterStart() {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            int port = server.getPort();
            assertThat(server.url()).isEqualTo("http://localhost:" + port + "/");
        }
    }

    @Test
    void urlWithPathAfterStart() {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            int port = server.getPort();
            assertThat(server.url("/customers")).isEqualTo("http://localhost:" + port + "/customers");
            assertThat(server.url("customers")).isEqualTo("http://localhost:" + port + "/customers");
        }
    }

    @Test
    void portPriorStart() {
        try (MockWebServer server = new MockWebServer()) {
            assertThat(server.getPort()).isEqualTo(-1);
        }
    }

    @Test
    void portAfterStart() {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            assertThat(server.getPort()).isGreaterThan(0);
        }
    }
}
