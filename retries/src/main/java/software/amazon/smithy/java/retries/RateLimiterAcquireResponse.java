/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.time.Duration;

/**
 * The result of a {@link RateLimiterTokenBucket#acquirePermit()} call.
 *
 * @param delay The amount of time to wait before making the request.
 */
record RateLimiterAcquireResponse(Duration delay) {}
