/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link RateLimiterTokenBucket} keeps track of past throttling responses and adapts to slow down the send rate to
 * adapt to the service. It does this by suggesting a delay amount as a result of an {@link #acquirePermit()} call. Callers
 * must update its internal state by calling {@link #updateRateAfterThrottling()} when getting a throttling response or
 * {@link #updateRateAfterSuccess()} when getting successful response.
 *
 * <p>This class is thread-safe, its internal current state is kept in the inner class {@link PersistentState} which is
 * stored using an {@link AtomicReference}. This class is converted to {@link TransientState} when the state needs to be
 * mutated and converted back to a {@link PersistentState} and stored using {@link AtomicReference#compareAndSet(Object,
 * Object)}.
 */
final class RateLimiterTokenBucket {
    final AtomicReference<PersistentState> stateReference;
    final AtomicLong requestCount;
    final RateLimiterClock clock;

    RateLimiterTokenBucket(RateLimiterClock clock) {
        this.clock = clock;
        this.stateReference = new AtomicReference<>(new PersistentState());
        this.requestCount = new AtomicLong(0);
    }

    /**
     * Acquire tokens from the bucket. If the bucket contains enough capacity to satisfy the request, this method will
     * return a {@link Duration#ZERO} value, otherwise it will return the amount of time the callers need to wait until
     * enough tokens are refilled.
     */
    Duration acquirePermit() {
        PersistentState current;
        PersistentState updated;
        Duration result;
        do {
            current = stateReference.get();
            var ts = current.toTransient();
            result = ts.tokenBucketAcquire(clock, 1.0);
            updated = ts.toPersistent();
        } while (!stateReference.compareAndSet(current, updated));
        return result;
    }

    /**
     * Updates the estimated send rate after a throttling response.
     */
    void updateRateAfterThrottling() {
        updateRateAfterThrottling(null);
    }

    /**
     * Updates the estimated send rate after a throttling response.
     */
    void updateRateAfterThrottling(RateLimiterUpdateResult result) {
        var count = requestCount.incrementAndGet();
        PersistentState current, updated;
        do {
            current = stateReference.get();
            var ts = current.toTransient();
            ts.updateClientSendingRate(clock, true, count, requestCount);
            updated = ts.toPersistent();
        } while (!stateReference.compareAndSet(current, updated));
        if (result != null) {
            result.setMeasuredTxRate(updated.measuredTxRate);
            result.setFillRate(updated.fillRate);
        }
    }

    /**
     * Updates the estimated send rate after a successful response.
     */
    void updateRateAfterSuccess() {
        updateRateAfterSuccess(null);
    }

    /**
     * Updates the estimated send rate after a successful response.
     */
    void updateRateAfterSuccess(RateLimiterUpdateResult result) {
        var count = requestCount.incrementAndGet();
        PersistentState current, updated;
        do {
            current = stateReference.get();
            var ts = current.toTransient();
            ts.updateClientSendingRate(clock, false, count, requestCount);
            updated = ts.toPersistent();
        } while (!stateReference.compareAndSet(current, updated));
        if (result != null) {
            result.setMeasuredTxRate(updated.measuredTxRate);
            result.setFillRate(updated.fillRate);
        }
    }

    /**
     * Mutable view of the rate limiter state used during state transitions. Created from a {@link PersistentState},
     * mutated in place, then converted back to a new {@link PersistentState}.
     */
    private static final class TransientState {
        private static final double MIN_FILL_RATE = 0.5;
        private static final double MIN_CAPACITY = 1.0;
        private static final double SMOOTH = 0.8;
        private static final double BETA = 0.7;
        private static final double SCALE_CONSTANT = 0.4;
        private double fillRate;
        private double maxCapacity;
        private double currentCapacity;
        private boolean lastTimestampIsSet;
        private double lastTimestamp;
        private boolean enabled;
        private double measuredTxRate;
        private double lastTxRateBucket;
        private double lastMaxRate;
        private double lastThrottleTime;
        private double timeWindow;
        private double newTokenBucketRate;

        private TransientState(PersistentState state) {
            this.fillRate = state.fillRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.measuredTxRate = state.measuredTxRate;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        PersistentState toPersistent() {
            return new PersistentState(this);
        }

        /**
         * Acquire tokens from the bucket. If the bucket contains enough capacity to satisfy the request, this method
         * will return a {@link Duration#ZERO} value, otherwise it will return the amount of time the callers need to
         * wait until enough tokens are refilled.
         */
        Duration tokenBucketAcquire(RateLimiterClock clock, double amount) {
            if (!this.enabled) {
                return Duration.ZERO;
            }
            refill(clock);
            var waitTime = 0.0;
            if (this.currentCapacity < amount) {
                waitTime = (amount - this.currentCapacity) / this.fillRate;
            }
            this.currentCapacity -= amount;
            return Duration.ofNanos((long) (waitTime * 1_000_000_000.0));
        }

        /**
         * Updates the sending rate depending on whether the response was successful or we got a throttling response.
         */
        void updateClientSendingRate(
                RateLimiterClock clock,
                boolean throttlingResponse,
                long count,
                AtomicLong requestCount
        ) {
            updateMeasuredRate(clock, count, requestCount);
            double calculatedRate;
            if (throttlingResponse) {
                double rateToUse;
                if (!this.enabled) {
                    rateToUse = this.measuredTxRate;
                } else {
                    rateToUse = Math.min(this.measuredTxRate, this.fillRate);
                }

                this.lastMaxRate = rateToUse;
                calculateTimeWindow();
                this.lastThrottleTime = clock.time();
                calculatedRate = cubicThrottle(rateToUse);
                this.enabled = true;
            } else {
                calculateTimeWindow();
                calculatedRate = cubicSuccess(clock.time());
            }

            var newRate = Math.min(calculatedRate, 2 * this.measuredTxRate);
            updateRate(clock, newRate);
        }

        void refill(RateLimiterClock clock) {
            var timestamp = clock.time();
            if (this.lastTimestampIsSet) {
                var fillAmount = (timestamp - this.lastTimestamp) * this.fillRate;
                this.currentCapacity = Math.min(this.maxCapacity, this.currentCapacity + fillAmount);
            }
            this.lastTimestamp = timestamp;
            this.lastTimestampIsSet = true;
        }

        void updateRate(RateLimiterClock clock, double newRps) {
            refill(clock);
            this.fillRate = Math.max(newRps, MIN_FILL_RATE);
            this.maxCapacity = Math.max(newRps, MIN_CAPACITY);
            this.currentCapacity = Math.min(this.currentCapacity, this.maxCapacity);
            this.newTokenBucketRate = newRps;
        }

        void updateMeasuredRate(RateLimiterClock clock, long count, AtomicLong requestCount) {
            var time = clock.time();
            var timeBucket = Math.floor(time * 2) / 2;
            if (timeBucket > this.lastTxRateBucket) {
                var currentRate = count / (timeBucket - this.lastTxRateBucket);
                this.measuredTxRate = (currentRate * SMOOTH) + (this.measuredTxRate * (1 - SMOOTH));
                requestCount.set(0);
                this.lastTxRateBucket = timeBucket;
            }
        }

        void calculateTimeWindow() {
            this.timeWindow = Math.pow((this.lastMaxRate * (1 - BETA)) / SCALE_CONSTANT, 1.0 / 3);
        }

        double cubicSuccess(double timestamp) {
            var delta = timestamp - this.lastThrottleTime;
            return (SCALE_CONSTANT * Math.pow(delta - this.timeWindow, 3)) + this.lastMaxRate;
        }

        double cubicThrottle(double rateToUse) {
            return rateToUse * BETA;
        }
    }

    /**
     * Immutable snapshot of the rate limiter state stored in an {@link AtomicReference}.
     */
    private static final class PersistentState {
        final double fillRate;
        final double measuredTxRate;
        final double maxCapacity;
        final double currentCapacity;
        final boolean lastTimestampIsSet;
        final double lastTimestamp;
        final boolean enabled;
        final double lastTxRateBucket;
        final double lastMaxRate;
        final double lastThrottleTime;
        final double timeWindow;
        final double newTokenBucketRate;

        private PersistentState() {
            this.fillRate = 0;
            this.measuredTxRate = 0;
            this.maxCapacity = 0;
            this.currentCapacity = 0;
            this.lastTimestampIsSet = false;
            this.lastTimestamp = 0;
            this.enabled = false;
            this.lastTxRateBucket = 0;
            this.lastMaxRate = 0;
            this.lastThrottleTime = 0;
            this.timeWindow = 0;
            this.newTokenBucketRate = 0;
        }

        PersistentState(TransientState state) {
            this.fillRate = state.fillRate;
            this.measuredTxRate = state.measuredTxRate;
            this.maxCapacity = state.maxCapacity;
            this.currentCapacity = state.currentCapacity;
            this.lastTimestampIsSet = state.lastTimestampIsSet;
            this.lastTimestamp = state.lastTimestamp;
            this.enabled = state.enabled;
            this.lastTxRateBucket = state.lastTxRateBucket;
            this.lastMaxRate = state.lastMaxRate;
            this.lastThrottleTime = state.lastThrottleTime;
            this.timeWindow = state.timeWindow;
            this.newTokenBucketRate = state.newTokenBucketRate;
        }

        TransientState toTransient() {
            return new TransientState(this);
        }
    }
}
