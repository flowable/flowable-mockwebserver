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
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import org.microhttp.EventLoop;
import org.microhttp.Handler;
import org.microhttp.Options;
import org.microhttp.OptionsBuilder;
import org.microhttp.Request;
import org.microhttp.Response;

/**
 * A simple mock web server that can be used to test HTTP clients.
 *
 * @author Filip Hrisafov
 */
public final class MockWebServer {

    private final TestHandler handler;
    private EventLoop eventLoop;
    private Options options;

    /**
     * Create a new mock webserver with a queue based response provider.
     */
    public MockWebServer() {
        this(new QueueResponseProvider());
    }

    /**
     * Create a new mock webserver with a custom response provider.
     *
     * @param responseProvider the custom response provider
     */
    public MockWebServer(Function<RecordedRequest, MockResponse> responseProvider) {
        this.handler = new TestHandler(responseProvider);
    }

    /**
     * Start the server on a random port.
     * Use {@link #getPort()} to get the port the server is running on.
     */
    public void start() {
        start(0);
    }

    /**
     * Start the server on the provided port.
     * If the server is already started this is a noop.
     *
     * @param port the port to start the server on
     */
    public void start(int port) {
        if (eventLoop == null) {
            options = OptionsBuilder.newBuilder().withPort(port).build();
            try {

                eventLoop = new EventLoop(options, handler);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to start server", e);
            }
            eventLoop.start();
        }
    }

    /**
     * Shutdown the server.
     */
    public void shutdown() {
        if (eventLoop == null) {
            return;
        }

        eventLoop.stop();
        try {
            eventLoop.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        eventLoop = null;

    }

    /**
     * The URL for connecting to this server.
     * This will return something like {@code http://<host>:<port>/}.
     *
     * @return a URL for connecting to this server under the {@code /} path.
     */
    public String url() {
        return url("/");
    }

    /**
     * Return a URL for connecting to this server using the given path.
     * This will return something like {@code http://<host>:<port><path>}.
     * Note if the path does not start with {@code /} it will be added.
     *
     * @param path the request path, such as "/v1".
     * @return a URL for connecting to this server using the given path.
     */
    public String url(String path) {
        if (eventLoop == null) {
            throw new IllegalStateException("Server not started");
        }

        try {
            return "http://" + options.host() + ":" + eventLoop.getPort() + (path.startsWith("/") ? path : "/" + path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sets the default response to return HTTP 501 (Not Implemented) if no other response is available.
     */
    public void failFast() {
        defaultResponse(MockResponse.newBuilder().status(MockHttpStatus.NOT_IMPLEMENTED).build());
    }

    /**
     * Sets the default response that will be returned if no other response is available.
     *
     * @param response the default response
     */
    public void defaultResponse(MockResponse response) {
        ((QueueResponseProvider) this.handler.responseProvider).defaultResponse = response;
    }

    /**
     * Enqueue a response to be returned by the server.
     *
     * @param responseBuilder the builder to use to build the response
     */
    public void enqueue(MockResponseBuilder responseBuilder) {
        enqueue(responseBuilder.build());
    }

    /**
     * Enqueue a response to be returned by the server.
     *
     * @param response the response to return
     */
    public void enqueue(MockResponse response) {
        ((QueueResponseProvider) this.handler.responseProvider).responseQueue.add(response);
    }

    /**
     * Take the next request that was received by the server.
     * The call will block until a request is available.
     *
     * @return the next request
     */
    public RecordedRequest takeRequest() {
        return handler.requestQueue.poll();
    }

    /**
     * Take the next request that was received by the server.
     * The call will block until a request is available or the timeout is reached.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the next request
     * @throws InterruptedException if the thread is interrupted
     */
    public RecordedRequest takeRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return handler.requestQueue.poll(timeout, unit);
    }

    /**
     * Take the next request that was received by the server.
     * The call will block until a request is available or the timeout is reached.
     *
     * @param timeout the maximum time to wait
     * @return the next request
     * @throws InterruptedException if the thread is interrupted
     */
    public RecordedRequest takeRequest(Duration timeout) throws InterruptedException {
        return handler.requestQueue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    /**
     * Get the number of requests that were received by the server.
     *
     * @return the number of requests
     */
    public long requestCount() {
        return handler.requestCount();
    }

    /**
     * Clear all requests and responses that were received by the server.
     */
    public void clearRequestsAndResponses() {
        handler.requestQueue.clear();
        Function<RecordedRequest, MockResponse> responseProvider = handler.responseProvider;
        if (responseProvider instanceof QueueResponseProvider queueResponseProvider) {
            queueResponseProvider.responseQueue.clear();
        }
    }

    /**
     * Get the port the server is running on.
     *
     * @return the port the server is running on
     */
    public int getPort() {
        try {
            return eventLoop != null ? eventLoop.getPort() : -1;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get port", e);
        }
    }

    private static class QueueResponseProvider implements Function<RecordedRequest, MockResponse> {

        private final BlockingQueue<MockResponse> responseQueue = new LinkedBlockingQueue<>();
        private MockResponse defaultResponse;

        @Override
        public MockResponse apply(RecordedRequest recordedRequest) {
            if (responseQueue.isEmpty() && defaultResponse != null) {
                return defaultResponse;
            } else {
                return responseQueue.poll();
            }
        }
    }

    private static class TestHandler implements Handler {

        private final Function<RecordedRequest, MockResponse> responseProvider;
        private final BlockingQueue<RecordedRequest> requestQueue = new LinkedBlockingQueue<>();
        private final AtomicLong requestCount = new AtomicLong(0);

        private TestHandler(Function<RecordedRequest, MockResponse> responseProvider) {
            this.responseProvider = responseProvider;
        }

        @Override
        public void handle(Request request, Consumer<Response> callback) {
            requestCount.incrementAndGet();
            RecordedRequest recordedRequest = new RecordedRequest(request);
            requestQueue.add(recordedRequest);
            MockResponse response = responseProvider.apply(recordedRequest);
            if (response != null) {
                handle(response, callback);
            }
        }

        private void handle(MockResponse response, Consumer<Response> callback) {
            Duration delay = response.delay;
            if (delay != null) {
                long sleep = delay.toMillis();
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }

            callback.accept(response.response);

        }

        private long requestCount() {
            return requestCount.get();
        }
    }

}
