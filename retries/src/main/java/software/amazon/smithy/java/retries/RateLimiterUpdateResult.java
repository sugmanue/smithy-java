/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.util.Objects;

/**
 * The result of a rate limiter update operation. This class is only used for testing.
 */
final class RateLimiterUpdateResult {
    private double measuredTxRate = -1;
    private double fillRate = -1;

    /**
     * Creates a new RateLimiterUpdateResponse.
     */
    RateLimiterUpdateResult() {}

    /**
     * Returns the measured transaction rate.
     *
     * @return the measured transaction rate
     */
    public double measuredTxRate() {
        return measuredTxRate;
    }

    /**
     * Returns current fill rate
     *
     * @return current fill rate
     */
    public double fillRate() {
        return fillRate;
    }

    /**
     * Sets the measured transaction rate.
     *
     * @param measuredTxRate measured transaction rate.
     * @return this object
     */
    public RateLimiterUpdateResult setMeasuredTxRate(double measuredTxRate) {
        this.measuredTxRate = measuredTxRate;
        return this;
    }

    /**
     * Sets the fill rate.
     *
     * @param fillRate fill rate.
     * @return this object
     */
    public RateLimiterUpdateResult setFillRate(double fillRate) {
        this.fillRate = fillRate;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (RateLimiterUpdateResult) obj;
        return Double.doubleToLongBits(this.measuredTxRate) == Double.doubleToLongBits(that.measuredTxRate) &&
                Double.doubleToLongBits(this.fillRate) == Double.doubleToLongBits(that.fillRate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(measuredTxRate, fillRate);
    }

    @Override
    public String toString() {
        return "RateLimiterUpdateResult{" +
                "measuredTxRate=" + measuredTxRate +
                ", fillRate=" + fillRate +
                '}';
    }
}
