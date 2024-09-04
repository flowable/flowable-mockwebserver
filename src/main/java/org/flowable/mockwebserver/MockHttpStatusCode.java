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

/**
 * Represents an HTTP status code.
 *
 * @author Filip Hrisafov
 * @see MockHttpStatus for known status codes
 * @see #from(int) for custom status codes
 */
public sealed interface MockHttpStatusCode permits MockHttpStatus, CustomMockHttpStatus {

    /**
     * @return The HTTP status code
     */
    int code();

    /**
     * @return The reason for the HTTP status code
     */
    String reason();

    /**
     * Create a {@link MockHttpStatusCode} from the given code
     *
     * @param code The HTTP status code
     * @return The {@link MockHttpStatusCode} for the given code
     */
    static MockHttpStatusCode from(int code) {
        return switch (code) {
            case 200 -> MockHttpStatus.OK;
            case 201 -> MockHttpStatus.CREATED;
            case 202 -> MockHttpStatus.ACCEPTED;
            case 204 -> MockHttpStatus.NO_CONTENT;
            case 300 -> MockHttpStatus.MULTIPLE_CHOICES;
            case 301 -> MockHttpStatus.MOVED_PERMANENTLY;
            case 302 -> MockHttpStatus.FOUND;
            case 303 -> MockHttpStatus.SEE_OTHER;
            case 304 -> MockHttpStatus.NOT_MODIFIED;
            case 307 -> MockHttpStatus.TEMPORARY_REDIRECT;
            case 308 -> MockHttpStatus.PERMANENT_REDIRECT;
            case 400 -> MockHttpStatus.BAD_REQUEST;
            case 401 -> MockHttpStatus.UNAUTHORIZED;
            case 402 -> MockHttpStatus.PAYMENT_REQUIRED;
            case 403 -> MockHttpStatus.FORBIDDEN;
            case 404 -> MockHttpStatus.NOT_FOUND;
            case 405 -> MockHttpStatus.METHOD_NOT_ALLOWED;
            case 406 -> MockHttpStatus.NOT_ACCEPTABLE;
            case 409 -> MockHttpStatus.CONFLICT;
            case 411 -> MockHttpStatus.LENGTH_REQUIRED;
            case 413 -> MockHttpStatus.PAYLOAD_TOO_LARGE;
            case 414 -> MockHttpStatus.URI_TOO_LONG;
            case 415 -> MockHttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case 418 -> MockHttpStatus.I_AM_A_TEAPOT;
            case 429 -> MockHttpStatus.TOO_MANY_REQUESTS;
            case 500 -> MockHttpStatus.INTERNAL_SERVER_ERROR;
            case 501 -> MockHttpStatus.NOT_IMPLEMENTED;
            case 502 -> MockHttpStatus.BAD_GATEWAY;
            case 503 -> MockHttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> MockHttpStatus.GATEWAY_TIMEOUT;
            default -> from(code, reasonForCode(code));
        };
    }

    static MockHttpStatusCode from(int code, String reason) {
        return new CustomMockHttpStatus(code, reason);
    }

    private static String reasonForCode(int code) {
        String reason = "Mock Response";
        if (code >= 100 && code < 200) {
            reason = "Informational";
        } else if (code >= 200 && code < 300) {
            reason = "OK";
        } else if (code >= 300 && code < 400) {
            reason = "Redirection";
        } else if (code >= 400 && code < 500) {
            reason = "Client Error";
        } else if (code >= 500 && code < 600) {
            reason = "Server Error";
        }
        return reason;
    }

}
