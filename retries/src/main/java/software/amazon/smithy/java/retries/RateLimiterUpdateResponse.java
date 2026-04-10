/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

/**
 * The result of a rate limiter update operation.
 *
 * @param measuredTxRate The measured transaction rate.
 * @param fillRate       The current fill rate.
 */
record RateLimiterUpdateResponse(double measuredTxRate, double fillRate) {}
