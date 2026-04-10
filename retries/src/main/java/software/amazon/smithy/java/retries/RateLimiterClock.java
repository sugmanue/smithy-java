/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

/**
 * A clock used by the rate limiter to measure elapsed time.
 */
interface RateLimiterClock {
    /**
     * Returns the current time in seconds with sub-second resolution. This clock needs not be related to the actual
     * wall clock time as it is only used to measure elapsed time.
     *
     * <p>For instance, if the current time is {@code PT2M8.067S} (2 minutes, 8 seconds, and 67 milliseconds), this
     * method returns {@code 128.067}.
     *
     * @return the current time in seconds with sub-second resolution.
     */
    double time();
}
