# Flowable Mock Web Server


[![Latest Version](https://img.shields.io/maven-central/v/org.flowable.mockwebserver/mockwebserver.svg?maxAge=3600&label=Latest%20Release)](https://central.sonatype.com/search?q=g:org.flowable.mockwebserver)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellowgreen.svg)](https://github.com/flowable/flowable-mockwebserver/blob/main/LICENSE.txt)

[![Build Status](https://github.com/flowable/flowable-mockwebserver/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/flowable/flowable-mockwebserver/actions?query=branch%3Amain+workflow%3ACI)

Web Server for testing HTTP clients

### Motivation

This library is inspired by the [OkHttp MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver).
It allows to specify which the responses that the server should return for each request and to verify the requests that the server received.

The reason for creating this library is that we wanted to have a similar functionality for testing HTTP clients in Java, but without the need to include the whole OkHttp library.
We are using a light weight webserver [Microhttp](https://github.com/ebarlas/microhttp) to handle the requests.
The use of Microhttp limits the functionality of the server (i.e. there is no HTTPS / TLS support and no HTTP 2 support), but it is enough for our needs.

There is also a different library [WireMock](http://wiremock.org/) that provides similar functionality, but that one is even more complex (with more dependencies) and has more features than we needed.

### Example

Here is a complete example

```java
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
```

### API

#### MockResponse

MockResponse is used to specify the response that the server should return for the next request.
By default, an empty response with status code 200 is returned.
They are created using `MockResponseBuilder` and you can customize it using a fluent API.

```java
MockResponse response = MockResponse.newBuilder()
        .jsonBody("""
                {
                  "id": 1,
                  "name": "Loki",
                  "type": "dog"
                }
                """)
        .header("Cache-Control", "no-cache")
        .build();
```

Responses can also be throttled, to simulate a slow network.

```java
MockResponse response = MockResponse.newBuilder()
        .jsonBody("{}")
        .bodyDelay(Duration.ofSeconds(2))
        .build();
```

The status of the response can be configured via `MockResponse.status` / `MockResponse.statusCode`.
It can directly be set using an integer value of the status or through some of the known statuses in `MockHttpStatus`

#### MockWebServer

The main entry point where response can be queued and requests can be retrieved to be verified.
It can be started on a random port (using `start()`) or on a specific port (`start(31313)`).
The URL of a specific path on the server can be retrieved using `url("/path")`.

```java
MockWebServer server = new MockWebServer();
server.start();
int port = server.getPort();
String petsUrl = server.url("/pets");
```

Responses can be enqueued directly using the `MockResponseBuilder` or `MockResponse`

e.g.

```java
server.enqueue(MockResponse.newBuilder()
    .jsonBody("""
        {
          "message": "Not Found"
        }
        """)
    .notFound());
```

```java
MockResponse response = MockResponse.newBuilder()
    .jsonBody("""
        {
          "message": "Not Found"
        }
        """)
    .notFound()
    .build();
server.enqueue(response);
```
Once you are done with the server it can be shutdown via `server.shutdown()`.

It is also possible to provide a custom (non queue based) response provider.

```java
Function<RecordedRequest, MockResponse> customResponseProvider = request -> switch (request.path()) {
    case "/login/auth/":
        return MockResponse.newBuilder().build();
    case "/check/version/":
        return MockResponse.newBuilder().body("version=9");
    case "/pets/1":
        return MockResponse.newBuilder().jsonBody("""
                {
                  "id": 1,
                  "name": "Loki",
                  "type": "dog"
                }
                """);
    default:
        return MockResponse.newBuilder().notFound().build();
};
```

#### RecordedRequest

RecordedRequest is used to verify the requests that the server received.
They can be retrieved using `server.takeRequest()` (which returns immediately) or `server.takeRequest(Duration.ofSeconds(5))` (which will wait 5 seconds for a request to be available before returning `null`).


```java
RecordedRequest recordedRequest = server.takeRequest();
assertThat(recordedRequest.path()).isEqualTo("/pets/1");
assertThat(recordedRequest.method()).isEqualTo("PUT");
assertThat(recordedRequest.header("Content-Type")).isEqualTo("application/json");
assertThat(recordedRequest.headerValues("Content-Type")).containsExactly("application/json");
assertThat(recordedRequest.body().asString()).isEqualTo("""
        {
          "name": "Loki Updated",
          "type": "dog"
        }
        """);
```
