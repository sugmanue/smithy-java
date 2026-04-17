/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lock-free implementation of a token bucket. Tokens can be acquired from the bucket as long as there is sufficient
 * capacity in the bucket.
 */
final class TokenBucket {
    private final int initialRetryTokens;
    private final AtomicInteger capacity;

    /**
     * Create a bucket containing the specified number of tokens.
     */
    TokenBucket(int initialRetryTokens) {
        this.initialRetryTokens = initialRetryTokens;
        this.capacity = new AtomicInteger(initialRetryTokens);
    }

    /**
     * Try to acquire a certain number of tokens from this bucket. If there aren't sufficient tokens in this bucket then
     * {@link AcquireResponse#acquisitionFailed()} returns {@code true}.
     */
    AcquireResponse tryAcquire(int amountToAcquire, DefaultRetryToken token) {
        if (amountToAcquire < 0) {
            throw new IllegalArgumentException("amountToAcquire cannot be negative");
        }

        if (amountToAcquire == 0) {
            return new AcquireResponse(initialRetryTokens, 0, 0, capacity.get(), false);
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();
            newCapacity = currentCapacity - amountToAcquire;
            if (newCapacity < 0) {
                var acquisitionFailed = !token.isLongPolling();
                return new AcquireResponse(initialRetryTokens, amountToAcquire, 0, currentCapacity, acquisitionFailed);
            }
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));

        return new AcquireResponse(initialRetryTokens, amountToAcquire, amountToAcquire, newCapacity, false);
    }

    /**
     * Release a number of tokens back to this bucket. If this number of tokens would exceed the maximum number of
     * tokens configured for the bucket, the bucket is instead set to the maximum value and the additional tokens are
     * discarded.
     */
    ReleaseResponse release(int amountToRelease) {
        if (amountToRelease <= 0) {
            throw new IllegalArgumentException("amountToRelease cannot be negative or zero");
        }

        int currentCapacity;
        int newCapacity;
        do {
            currentCapacity = capacity.get();
            newCapacity = Math.min(currentCapacity + amountToRelease, initialRetryTokens);
        } while (!capacity.compareAndSet(currentCapacity, newCapacity));

        return new ReleaseResponse(amountToRelease, newCapacity, initialRetryTokens);
    }

    /**
     * Retrieve a snapshot of the current number of tokens in the bucket. Because this number is constantly changing,
     * it's recommended to refer to the {@link AcquireResponse#capacityRemaining()} returned by the {@link
     * #tryAcquire(int)} method whenever possible.
     */
    int currentCapacity() {
        return capacity.get();
    }

    /**
     * Retrieve the maximum capacity of the bucket configured when the bucket was created.
     */
    int maxCapacity() {
        return initialRetryTokens;
    }

    @Override
    public String toString() {
        return "TokenBucket{" +
                "maxCapacity=" + initialRetryTokens +
                ", capacity=" + capacity.get() +
                '}';
    }
}
