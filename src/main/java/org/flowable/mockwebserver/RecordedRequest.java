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

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.microhttp.Header;
import org.microhttp.Request;

/**
 * The request that was received by the {@link MockWebServer}.
 *
 * @author Filip Hrisafov
 */
public class RecordedRequest {

    protected final Request request;

    public RecordedRequest(Request request) {
        this.request = request;
    }

    /**
     * The raw path of the request.
     * This does not include the scheme, host and port of the request.
     * It only includes the path and the query parameters.
     *
     * @return The path of the request.
     */
    public String path() {
        return request.uri();
    }

    /**
     * The parsed request URL.
     * This can be used if you want to get access to the path and the query parameters separately.
     *
     * @return The parsed request URL.
     */
    public RequestUrl requestUrl() {
        return RequestUrl.from(path());
    }

    /**
     * @return the method of the request
     */
    public String method() {
        return request.method();
    }

    /**
     * Get the value of the first header with the provided name (ignoring case).
     *
     * @param name the name of the header
     * @return the value of the first header or {@code null} if no header with the provided name is present
     */
    public String header(String name) {
        return request.header(name);
    }

    /**
     * Get all the values of the headers with the provided name (ignoring case).
     *
     * @param name the name of the header
     * @return the values of the headers or an empty list if no headers with the provided name are present
     */
    public List<String> headerValues(String name) {
        List<String> headerValues = new ArrayList<>();
        for (Header header : request.headers()) {
            if (header.name().equalsIgnoreCase(name)) {
                headerValues.add(header.value());
            }
        }

        return headerValues;
    }

    /**
     * Get all the headers of the request.
     *
     * @return the headers of the request
     */
    public Map<String, List<String>> headers() {
        Map<String, List<String>> headers = new HashMap<>();
        for (Header header : request.headers()) {
            headers.computeIfAbsent(header.name(), k -> new ArrayList<>())
                    .add(header.value());
        }

        return headers;
    }

    /**
     * Get the body of the request.
     *
     * @return the body of the request
     */
    public Body body() {
        return new Body(request.body());
    }

    public static class Body {

        protected final byte[] bytes;

        protected Body(byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         * @return the body of the request as a string using the UTF-8 charset.
         */
        public String asString() {
            return asString(StandardCharsets.UTF_8);
        }

        /**
         * @return the body of the request as a string using give charset
         */
        public String asString(Charset charset) {
            return new String(bytes, charset);
        }

        /**
         * @return the body of the request as a byte array
         */
        public byte[] asByteArray() {
            return bytes;
        }
    }

    public record RequestUrl(String path, Map<String, String> queryParameters) {

        public RequestUrl {
            queryParameters = Collections.unmodifiableMap(queryParameters);
        }

        private static RequestUrl from(String uriString) {
            URI uri = URI.create(uriString);
            Map<String, String> queryParameters = new HashMap<>();
            String query = uri.getQuery();
            if (query != null) {
                String[] queryParams = query.split("&");
                for (String param : queryParams) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        queryParameters.put(key, value);
                    }
                }
            }
            return new RequestUrl(uri.getPath(), queryParameters);
        }
    }

}
