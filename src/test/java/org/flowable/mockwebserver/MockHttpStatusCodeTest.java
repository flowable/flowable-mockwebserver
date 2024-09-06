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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Filip Hrisafov
 */
class MockHttpStatusCodeTest {

    @ParameterizedTest
    @MethodSource("unknownStatusCodes")
    void fromUnknown(int statusCode, String expectedReason) {
        MockHttpStatusCode status = MockHttpStatusCode.from(statusCode);

        assertThat(status).isNotNull();
        assertThat(status.code()).isEqualTo(statusCode);
        assertThat(status.reason()).isEqualTo(expectedReason);

    }

    @ParameterizedTest
    @EnumSource
    void fromKnownWithCustomReason(MockHttpStatus status) {
        MockHttpStatusCode statusCode = MockHttpStatusCode.from(status.code(), "Custom " + status.code() + " reason");

        assertThat(statusCode).isNotNull();
        assertThat(statusCode.code()).isEqualTo(status.code());
        assertThat(statusCode.reason()).isEqualTo("Custom " + status.code() + " reason");

    }

    static Stream<Arguments> unknownStatusCodes() {
        return Stream.of(
                Arguments.of(100, "Informational"),
                Arguments.of(150, "Informational"),
                Arguments.of(228, "OK"),
                Arguments.of(356, "Redirection"),
                Arguments.of(458, "Client Error"),
                Arguments.of(560, "Server Error"),
                Arguments.of(601, "Mock Response")
        );
    }

    static Stream<Arguments> knownStatusCodesWithCustomReason() {
        return Stream.of(MockHttpStatus.values())
                .map(status -> Arguments.of(status.code(), "Custom " + status.code() + " reason"));
    }
}
