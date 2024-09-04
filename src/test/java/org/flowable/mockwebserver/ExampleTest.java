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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ExampleTest {

    protected MockWebServer server;

    @BeforeEach
    void setUp() {
        server = new MockWebServer();
        server.start();
        server.failFast();
    }

    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    @Test
    void queryPets() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        server.enqueue(MockResponse.newBuilder().jsonBody("""
                [
                  {
                    "id": 1,
                    "name": "Loki",
                    "type": "dog"
                  },
                  {
                    "id": 2,
                    "name": "Garfield",
                    "type": "cat"
                  }
                ]
                """));
        server.enqueue(MockResponse.newBuilder().jsonBody("""
                {
                  "message": "Not Found"
                }
                """).notFound());

        String petsUrl = server.url("/pets");
        HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(petsUrl))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValue("application/json");
        assertThat(response.body())
                .isEqualTo("""
                        [
                          {
                            "id": 1,
                            "name": "Loki",
                            "type": "dog"
                          },
                          {
                            "id": 2,
                            "name": "Garfield",
                            "type": "cat"
                          }
                        ]
                        """);

        response = client.send(HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString("""
                        {
                          "name": "Loki Updated",
                          "type": "dog"
                        }
                        """))
                .uri(URI.create(petsUrl + "/1"))
                .build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).isEqualTo("""
                {
                  "message": "Not Found"
                }
                """);

        RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.path()).isEqualTo("/pets");
        assertThat(recordedRequest.method()).isEqualTo("GET");
        assertThat(recordedRequest.requestUrl().queryParameters()).isEmpty();

        recordedRequest = server.takeRequest();
        assertThat(recordedRequest.path()).isEqualTo("/pets/1");
        assertThat(recordedRequest.method()).isEqualTo("PUT");
        assertThat(recordedRequest.requestUrl().queryParameters()).isEmpty();
        assertThat(recordedRequest.body().asString())
                .isEqualTo("""
                        {
                          "name": "Loki Updated",
                          "type": "dog"
                        }
                        """);
    }
}
