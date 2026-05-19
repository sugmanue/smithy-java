/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * SEP Appendix A: CUBIC Testcases.
 *
 * <p>Verifies _CUBICSuccess() and _CUBICThrottle() calculations with BETA=0.7 and SCALE_CONSTANT=0.4.
 */
class SepCubicTest {
    private static final double EPSILON = 0.001;

    // SEP test set 1: all success responses, last_max_rate=10, last_throttle_time=5
    @Test
    void cubicAllSuccess() {
        double lastMaxRate = 10;
        double lastThrottleTime = 5;

        assertRate(lastMaxRate, lastThrottleTime, 5, 7.0);
        assertRate(lastMaxRate, lastThrottleTime, 6, 9.64893600966);
        assertRate(lastMaxRate, lastThrottleTime, 7, 10.000030849917364);
        assertRate(lastMaxRate, lastThrottleTime, 8, 10.453284520772092);
        assertRate(lastMaxRate, lastThrottleTime, 9, 13.408697022224185);
        assertRate(lastMaxRate, lastThrottleTime, 10, 21.26626835427364);
        assertRate(lastMaxRate, lastThrottleTime, 11, 36.425998516920465);
    }

    // SEP test set 2: mixed success/throttle, last_max_rate=10, last_throttle_time=5
    // State evolves through the sequence.
    @Test
    void cubicMixedSuccessAndThrottle() {
        record Step(String response, double timestamp, double expectedRate) {}

        var steps = List.of(
                new Step("success", 5, 7.0),
                new Step("success", 6, 9.64893600966),
                new Step("throttle", 7, 6.754255206761999),
                new Step("throttle", 8, 4.727978644733399),
                new Step("success", 9, 6.606547753887045),
                new Step("success", 10, 6.763279816944947),
                new Step("success", 11, 7.598174833907107),
                new Step("success", 12, 11.511232804773524));

        double lastMaxRate = 10;
        double lastThrottleTime = 5;
        double previousCalculatedRate = 0;

        for (var step : steps) {
            double calculatedRate;
            if ("throttle".equals(step.response)) {
                // _CUBICThrottle uses the previous calculated_rate as input
                calculatedRate = RateLimiterTokenBucket.computeCubicThrottle(previousCalculatedRate);
                // After throttle: last_max_rate = previous calculated_rate, last_throttle_time = now
                lastMaxRate = previousCalculatedRate;
                lastThrottleTime = step.timestamp;
            } else {
                calculatedRate = RateLimiterTokenBucket.computeCubicSuccess(
                        lastMaxRate,
                        lastThrottleTime,
                        step.timestamp);
            }
            assertThat(calculatedRate)
                    .as("response=%s, timestamp=%s", step.response, step.timestamp)
                    .isCloseTo(step.expectedRate, within(EPSILON));
            previousCalculatedRate = calculatedRate;
        }
    }

    private void assertRate(double lastMaxRate, double lastThrottleTime, double timestamp, double expected) {
        double rate = RateLimiterTokenBucket.computeCubicSuccess(lastMaxRate, lastThrottleTime, timestamp);
        assertThat(rate)
                .as("timestamp=%s", timestamp)
                .isCloseTo(expected, within(EPSILON));
    }
}
