/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.time.Duration;

/**
 * Constants used by retry strategy implementations.
 */
final class Constants {
    static final Duration BASE_DELAY_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    static final Duration MAX_BACKOFF_CEILING = Duration.ofMillis(Integer.MAX_VALUE); // Around ~24.8 days
    /**
     * Max permitted retry times. To prevent exponentialDelay from overflow, there must be 2 ^ retriesAttempted &lt;= 2 ^ 31 - 1,
     * which means retriesAttempted &lt;= 30, so that is the ceil for retriesAttempted.
     */
    static final int RETRIES_ATTEMPTED_CEILING = (int) Math.floor(Math.log(Integer.MAX_VALUE) / Math.log(2));

    /**
     * Constants for the {@link StandardRetryStrategy}.
     */
    static final class Standard {
        static final Duration BASE_DELAY = Duration.ofMillis(50);
        static final Duration MAX_BACKOFF = Duration.ofSeconds(20);
        static final int MAX_ATTEMPTS = 3;
        static final int RETRY_COST = 14;
        static final int THROTTLING_RETRY_COST = 5;
        static final int INITIAL_RETRY_TOKENS = 500;
        static final int MAX_SCOPES = 128;
        private Standard() {}
    }

    /**
     * Constants for the {@link AdaptiveRetryStrategy}.
     */
    static final class Adaptive {
        static final Duration BASE_DELAY = Standard.BASE_DELAY;
        static final Duration MAX_BACKOFF = Standard.MAX_BACKOFF;
        static final int MAX_ATTEMPTS = Standard.MAX_ATTEMPTS;
        static final int RETRY_COST = Standard.RETRY_COST;
        static final int THROTTLING_RETRY_COST = Standard.THROTTLING_RETRY_COST;
        static final int INITIAL_RETRY_TOKENS = Standard.INITIAL_RETRY_TOKENS;
        static final int MAX_SCOPES = Standard.MAX_SCOPES;
        private Adaptive() {}
    }

    private Constants() {}
}
