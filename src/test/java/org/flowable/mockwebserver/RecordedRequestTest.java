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
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Filip Hrisafov
 */
class RecordedRequestTest {

    protected MockWebServer server = new MockWebServer();
    protected HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        server.start();
        server.failFast();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void pathWithQueryParameters() throws IOException, InterruptedException {
        httpClient.send(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(server.url("/pets") + "?name=Garfield&type=cat"))
                .build(), HttpResponse.BodyHandlers.ofString());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.path()).isEqualTo("/pets?name=Garfield&type=cat");
        RecordedRequest.RequestUrl requestUrl = recordedRequest.requestUrl();
        assertThat(requestUrl).isNotNull();
        assertThat(requestUrl.path()).isEqualTo("/pets");
        assertThat(requestUrl.queryParameters())
                .containsOnly(
                        entry("name", "Garfield"),
                        entry("type", "cat")
                );
    }

    @Test
    void headers() throws IOException, InterruptedException {
        httpClient.send(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(server.url("/pets")))
                .header("X-Custom-Header", "custom-value")
                .header("X-Custom-Header", "custom-value2")
                .header("Accept", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.headers())
                .contains(
                        entry("X-Custom-Header", List.of("custom-value", "custom-value2")),
                        entry("Accept", Collections.singletonList("application/json"))
                );
        assertThat(recordedRequest.header("X-Custom-Header")).isEqualTo("custom-value");
        assertThat(recordedRequest.header("x-custom-header")).isEqualTo("custom-value");
        assertThat(recordedRequest.headerValues("X-Custom-Header")).containsOnly("custom-value", "custom-value2");
        assertThat(recordedRequest.headerValues("x-custom-header")).containsOnly("custom-value", "custom-value2");
        assertThat(recordedRequest.header("X-Dummy")).isNull();
        assertThat(recordedRequest.headerValues("X-Dummy")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "POST", "PUT", "DELETE", "PATCH" })
    void method(String method) throws IOException, InterruptedException {
        httpClient.send(HttpRequest.newBuilder()
                .method(method, HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(server.url("/pets")))
                .build(), HttpResponse.BodyHandlers.ofString());

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.method()).isEqualTo(method);
    }

    @Test
    void body() throws IOException, InterruptedException {
        httpClient.send(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString("Hello World"))
                .uri(URI.create(server.url("/pets")))
                .build(), HttpResponse.BodyHandlers.ofString());

        RecordedRequest recordedRequest = server.takeRequest();
        RecordedRequest.Body body = recordedRequest.body();
        assertThat(body).isNotNull();
        assertThat(body.asString()).isEqualTo("Hello World");
        assertThat(body.asByteArray()).isEqualTo("Hello World".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void emptyRequestBody() throws IOException, InterruptedException {
        httpClient.send(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(server.url("/pets")))
                .build(), HttpResponse.BodyHandlers.ofString());

        RecordedRequest recordedRequest = server.takeRequest();
        RecordedRequest.Body body = recordedRequest.body();
        assertThat(body).isNotNull();
        assertThat(body.asString()).isEqualTo("");
        assertThat(body.asByteArray()).isEmpty();
    }
}
