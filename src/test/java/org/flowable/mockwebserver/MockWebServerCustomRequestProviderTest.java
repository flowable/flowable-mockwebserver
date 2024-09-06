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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class MockWebServerCustomRequestProviderTest {

    protected MockWebServer server = new MockWebServer(new CustomRequestProvider());
    protected HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeEach
    void setUp() {
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void customRequestProvider() throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString("Hello World"))
                .uri(URI.create(server.url()))
                .build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello World");
    }

    @Test
    void enqueue() {
        assertThatThrownBy(() -> server.enqueue(MockResponse.newBuilder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot enqueue response when using a custom response provider");
    }

    @Test
    void defaultResponse() {
        assertThatThrownBy(() -> server.defaultResponse(MockResponse.newBuilder().build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot set default response when using a custom response provider");
    }

    static class CustomRequestProvider implements Function<RecordedRequest, MockResponse> {

        @Override
        public MockResponse apply(RecordedRequest recordedRequest) {
            return MockResponse.newBuilder().status(MockHttpStatus.OK).body(recordedRequest.body().asByteArray()).build();
        }
    }

}
