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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ResponseDelaySchedulerTest {

    @Test
    void scheduleRunsTaskWithoutBlockingCaller() throws InterruptedException {
        ResponseDelayScheduler scheduler = new ResponseDelayScheduler();
        scheduler.start();
        try {
            CountDownLatch latch = new CountDownLatch(1);
            long start = System.currentTimeMillis();
            scheduler.schedule(latch::countDown, Duration.ofMillis(300));
            long afterSchedule = System.currentTimeMillis();

            assertThat(afterSchedule - start).isLessThan(100);
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void cancelPendingPreventsScheduledTaskFromRunning() throws InterruptedException {
        ResponseDelayScheduler scheduler = new ResponseDelayScheduler();
        scheduler.start();
        try {
            AtomicBoolean ran = new AtomicBoolean(false);
            scheduler.schedule(() -> ran.set(true), Duration.ofMillis(500));
            scheduler.cancelPending();

            Thread.sleep(800);
            assertThat(ran).isFalse();
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    void scheduleWhenNotStartedThrows() {
        ResponseDelayScheduler scheduler = new ResponseDelayScheduler();
        assertThatThrownBy(() -> scheduler.schedule(() -> { }, Duration.ofMillis(10)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scheduleAfterShutdownThrows() {
        ResponseDelayScheduler scheduler = new ResponseDelayScheduler();
        scheduler.start();
        scheduler.shutdown();
        assertThatThrownBy(() -> scheduler.schedule(() -> { }, Duration.ofMillis(10)))
                .isInstanceOf(IllegalStateException.class);
    }
}
