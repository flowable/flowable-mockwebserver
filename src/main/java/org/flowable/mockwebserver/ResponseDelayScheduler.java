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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules deferred response sends off the event loop thread, so that a delayed
 * response never blocks the server and can be cancelled cleanly.
 *
 * @author Filip Hrisafov
 */
final class ResponseDelayScheduler {

    private volatile ScheduledExecutorService executor;
    private final Set<Future<?>> pending = ConcurrentHashMap.newKeySet();

    void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mock-web-server-response-delay");
            thread.setDaemon(true);
            return thread;
        });
    }

    void schedule(Runnable command, Duration delay) {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor == null) {
            throw new IllegalStateException("Response delay scheduler is not started");
        }

        pending.removeIf(Future::isDone);
        pending.add(currentExecutor.schedule(command, delay.toNanos(), TimeUnit.NANOSECONDS));
    }

    void cancelPending() {
        for (Future<?> future : pending) {
            future.cancel(false);
        }
        pending.clear();
    }

    void shutdown() {
        ScheduledExecutorService currentExecutor = executor;
        if (currentExecutor != null) {
            currentExecutor.shutdownNow();
            executor = null;
        }
        pending.clear();
    }
}
