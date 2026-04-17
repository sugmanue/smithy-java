/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import software.amazon.smithy.java.retries.api.AcquireInitialTokenFlags;
import software.amazon.smithy.java.retries.api.AcquireInitialTokenRequest;
import software.amazon.smithy.java.retries.api.RecordSuccessRequest;
import software.amazon.smithy.java.retries.api.RefreshRetryTokenRequest;
import software.amazon.smithy.java.retries.api.RefreshRetryTokenResponse;
import software.amazon.smithy.java.retries.api.RetryInfo;
import software.amazon.smithy.java.retries.api.RetrySafety;
import software.amazon.smithy.java.retries.api.RetryToken;
import software.amazon.smithy.java.retries.api.TokenAcquisitionFailedException;

public class RetryStrategyTestCommon {

    public static Collection<TestCase> parameters() {
        return Arrays.asList(
                builder("Cannot retry on non-retryable")
                        .statuses(CallStatus.NON_RETRYABLE)
                        .expectSuccess(false)
                        .expectedLastCapacityAcquired(0)
                        .expectedCapacityRemaining(500)
                        .build(),
                builder("Can retry on retryable")
                        .statuses(CallStatus.RETRYABLE)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(14)
                        .expectedCapacityRemaining(500 - 14)
                        .build(),
                builder("Can retry on and returns cost")
                        .statuses(CallStatus.RETRYABLE, CallStatus.SUCCESS)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(14)
                        .expectedCapacityRemaining(500)
                        .build(),
                builder("Can retry twice and returns cost")
                        .statuses(CallStatus.RETRYABLE, CallStatus.RETRYABLE, CallStatus.SUCCESS)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(14)
                        .expectedCapacityRemaining(500 - 14)
                        .build(),
                builder("Can retry on throttling")
                        .statuses(CallStatus.THROTTLED)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(5)
                        .expectedCapacityRemaining(500 - 5)
                        .build(),
                builder("Can retry on throttling twice and returns cost")
                        .statuses(CallStatus.THROTTLED, CallStatus.THROTTLED, CallStatus.SUCCESS)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(5)
                        .expectedCapacityRemaining(500 - 5)
                        .build(),
                builder("Can retry on two retryable")
                        .statuses(CallStatus.RETRYABLE, CallStatus.RETRYABLE)
                        .expectSuccess(true)
                        .expectedLastCapacityAcquired(14)
                        .expectedCapacityRemaining(500 - (14 * 2))
                        .build(),
                builder("Cannot retry on two retryable and one throttling")
                        .statuses(CallStatus.RETRYABLE, CallStatus.RETRYABLE, CallStatus.THROTTLED)
                        .expectSuccess(false)
                        .expectedLastCapacityAcquired(14)
                        .expectedCapacityRemaining(500 - (14 * 2))
                        .build(),
                builder("Exhausts tokens with retryable")
                        .statuses(createExhaustingRetryableCallStatuses())
                        .expectSuccess(false)
                        .expectedLastCapacityAcquired(0)
                        .expectedCapacityRemaining(10)
                        .build(),
                builder("Exhausts tokens with retryable yet succeeds for long polling")
                        .statuses(createExhaustingRetryableCallStatusesWithFinalSuccess())
                        .expectSuccess(true)
                        .isLongPolling(true)
                        .expectedLastCapacityAcquired(0)
                        .expectedCapacityRemaining(11) // returns 1 upon success
                        .build(),
                builder("Exhausts tokens with throttling")
                        .statuses(createExhaustingThrottlingCallStatuses())
                        .expectSuccess(false)
                        .expectedLastCapacityAcquired(5)
                        .expectedCapacityRemaining(0)
                        .build(),
                builder("Exhausts tokens with throttling yet succeeds for long polling")
                        .statuses(createExhaustingThrottlingCallStatusesWithFinalSuccess())
                        .expectSuccess(true)
                        .isLongPolling(true)
                        .expectedLastCapacityAcquired(0)
                        .expectedCapacityRemaining(1) // returns 1 upon success
                        .build());
    }

    static TestCaseBuilder builder(String name) {
        return new TestCaseBuilder(name);
    }

    static List<CallStatus> createExhaustingRetryableCallStatuses() {
        var result = new ArrayList<CallStatus>();

        for (var idx = 0; idx < 34; idx++) {
            result.add(CallStatus.RETRYABLE);
            result.add(CallStatus.RETRYABLE);
            result.add(CallStatus.SUCCESS);
        }
        result.add(CallStatus.RETRYABLE);
        result.add(CallStatus.RETRYABLE);
        return result;
    }

    static List<CallStatus> createExhaustingRetryableCallStatusesWithFinalSuccess() {
        var result = createExhaustingRetryableCallStatuses();
        result.add(CallStatus.SUCCESS);
        return result;
    }

    static List<CallStatus> createExhaustingThrottlingCallStatuses() {
        var result = new ArrayList<CallStatus>();
        for (var idx = 0; idx < 99; idx++) {
            result.add(CallStatus.THROTTLED);
            result.add(CallStatus.THROTTLED);
            result.add(CallStatus.SUCCESS);
        }
        result.add(CallStatus.THROTTLED);
        return result;
    }

    static List<CallStatus> createExhaustingThrottlingCallStatusesWithFinalSuccess() {
        var result = createExhaustingThrottlingCallStatuses();
        result.add(CallStatus.THROTTLED);
        result.add(CallStatus.SUCCESS);
        return result;
    }

    static class TestCase {
        final String name;
        final List<CallStatus> statuses;
        final boolean expectSuccess;
        final Integer expectedCapacityAcquired;
        final Integer expectedCapacityRemaining;
        final int flags;

        TestCase(TestCaseBuilder builder) {
            this.name = builder.name;
            this.statuses = builder.statuses;
            this.expectSuccess = builder.expectSuccess;
            this.expectedCapacityAcquired = builder.expectedCapacityAcquired;
            this.expectedCapacityRemaining = builder.expectedCapacityRemaining;
            this.flags = builder.flags;
        }

        public void run(BaseRetryStrategy strategy) {
            RetryToken token = null;
            var acquireInitialToken = true;
            for (var idx = 0; idx < statuses.size(); idx++) {
                var status = statuses.get(idx);
                if (acquireInitialToken) {
                    var initialAcquireResponse = strategy.acquireInitialToken(
                            new AcquireInitialTokenRequest("scope", flags));
                    token = initialAcquireResponse.token();
                }
                var exception = fromStatus(status);
                if (exception != null) {
                    var request = new RefreshRetryTokenRequest(token, exception, null);
                    RefreshRetryTokenResponse response;
                    try {
                        response = strategy.refreshRetryToken(request);
                        token = response.token();
                    } catch (TokenAcquisitionFailedException e) {
                        if (expectSuccess) {
                            throw new AssertionError("Expected token acquisition failed", e);
                        }
                        token = e.token();
                        if (idx != statuses.size() - 1) {
                            throw new AssertionError("Test case not setup correctly, " +
                                    "not all statuses were covered, remaining: " +
                                    statuses.subList(idx + 1, statuses.size()));
                        }
                    }
                    acquireInitialToken = false;
                } else {
                    var response = strategy.recordSuccess(new RecordSuccessRequest(token));
                    token = response.token();
                    acquireInitialToken = true;
                }
            }
            var defaultToken = (DefaultRetryToken) token;
            if (expectedCapacityAcquired != null) {
                assertEquals(expectedCapacityAcquired, defaultToken.capacityAcquired());
            }
            if (expectedCapacityRemaining != null) {
                assertEquals(expectedCapacityRemaining, defaultToken.capacityRemaining());
            }
        }

        Throwable fromStatus(CallStatus status) {
            return switch (status) {
                case RETRYABLE -> new CallException(true, false);
                case THROTTLED -> new CallException(true, true);
                case NON_RETRYABLE -> new RuntimeException("");
                case SUCCESS -> null;
            };
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class TestCaseBuilder {
        String name;
        List<CallStatus> statuses = new ArrayList<>();
        boolean expectSuccess;
        Integer expectedCapacityAcquired;
        Integer expectedCapacityRemaining;
        int flags = 0;

        TestCaseBuilder(String name) {
            this.name = name;
        }

        public TestCaseBuilder statuses(CallStatus... statuses) {
            this.statuses = Arrays.asList(statuses);
            return this;
        }

        public TestCaseBuilder statuses(List<CallStatus> statuses) {
            this.statuses = statuses;
            return this;
        }

        public TestCaseBuilder expectSuccess(boolean expectSuccess) {
            this.expectSuccess = expectSuccess;
            return this;
        }

        public TestCaseBuilder expectedLastCapacityAcquired(Integer expectedCapacityAcquired) {
            this.expectedCapacityAcquired = expectedCapacityAcquired;
            return this;
        }

        public TestCaseBuilder expectedCapacityRemaining(Integer expectedCapacityRemaining) {
            this.expectedCapacityRemaining = expectedCapacityRemaining;
            return this;
        }

        public TestCaseBuilder isLongPolling(boolean isLongPolling) {
            if (isLongPolling) {
                this.flags = this.flags | AcquireInitialTokenFlags.IS_LONG_POLLING;
            }
            return this;
        }

        public TestCase build() {
            return new TestCase(this);
        }
    }

    enum CallStatus {
        RETRYABLE,
        THROTTLED,
        NON_RETRYABLE,
        SUCCESS
    }

    static class CallException extends RuntimeException implements RetryInfo {
        private final boolean isThrottle;
        private final RetrySafety safety;

        CallException(boolean isRetryable, boolean isThrottle) {
            super("");
            this.isThrottle = isThrottle;
            if (isRetryable) {
                safety = RetrySafety.YES;
            } else {
                safety = RetrySafety.NO;
            }
        }

        @Override
        public RetrySafety isRetrySafe() {
            return safety;
        }

        @Override
        public boolean isThrottle() {
            return isThrottle;
        }

        @Override
        public Duration retryAfter() {
            return null;
        }
    }

}
