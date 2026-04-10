/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

/**
 * A {@link RateLimiterClock} backed by {@link System#nanoTime()}.
 */
final class SystemClock implements RateLimiterClock {
    @Override
    public double time() {
        return System.nanoTime() / 1_000_000_000.0;
    }
}
