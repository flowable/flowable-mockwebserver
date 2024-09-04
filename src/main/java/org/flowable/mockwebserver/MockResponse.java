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

import java.time.Duration;

import org.microhttp.Response;

/**
 * @author Filip Hrisafov
 */
public class MockResponse {

    protected final Response response;
    protected final Duration delay;

    protected MockResponse(Response response, Duration delay) {
        this.response = response;
        this.delay = delay;
    }

    /**
     * Create a new builder for a {@link MockResponse} with a default status of {@link MockHttpStatus#OK}
     */
    public static MockResponseBuilder newBuilder() {
        return MockResponseBuilder.newBuilder();
    }
}
