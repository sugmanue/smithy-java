/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

/**
 * The result of a token release operation on a {@link TokenBucket}.
 *
 * @param capacityReleased The number of tokens released.
 * @param currentCapacity  The capacity of the token bucket after the release.
 * @param maxCapacity      The maximum capacity of the token bucket.
 */
record ReleaseResponse(int capacityReleased, int currentCapacity, int maxCapacity) {}
