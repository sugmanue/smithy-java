/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

/**
 * The result of a token acquisition attempt on a {@link TokenBucket}.
 *
 * @param maxCapacity       The maximum capacity of the token bucket.
 * @param capacityRequested The number of tokens requested.
 * @param capacityAcquired  The number of tokens actually acquired.
 * @param capacityRemaining The number of tokens remaining in the bucket.
 * @param acquisitionFailed {@code true} if the requested capacity was not successfully acquired.
 */
record AcquireResponse(
        int maxCapacity,
        int capacityRequested,
        int capacityAcquired,
        int capacityRemaining,
        boolean acquisitionFailed) {}
