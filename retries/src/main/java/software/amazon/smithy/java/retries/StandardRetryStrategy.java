/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.time.Duration;
import software.amazon.smithy.java.logging.InternalLogger;
import software.amazon.smithy.java.retries.api.RetryStrategy;

/**
 * The standard retry strategy is the recommended {@link RetryStrategy} for normal use-cases.
 * <p>
 * Unlike {@link AdaptiveRetryStrategy}, the standard strategy is generally useful across all retry use-cases.
 * <p>
 * The standard retry strategy by default:
 * <ol>
 *     <li>Retries on the conditions configured in the {@link Builder}.
 *     <li>Retries 2 times (3 total attempts). Adjust with {@link Builder#maxAttempts(int)}
 *     <li>Uses the {@link ExponentialDelayWithJitter} backoff strategy, with a base delay of
 *     50 milliseconds and max delay of 20 seconds. Adjust with {@link Builder#backoffBaseDelay(Duration)}
 *     and {@link Builder#backoffMaxBackoff(Duration)}.
 * </ol>
 *
 * @see AdaptiveRetryStrategy
 */
public final class StandardRetryStrategy extends BaseRetryStrategy {
    private static final InternalLogger LOGGER = InternalLogger.getLogger(StandardRetryStrategy.class);

    StandardRetryStrategy(Builder builder) {
        super(LOGGER, builder);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Creates a new {@link Builder} with default configuration.
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder()
                .maxAttempts(Constants.Standard.MAX_ATTEMPTS)
                .backoffBaseDelay(Constants.Standard.BASE_DELAY)
                .backoffMaxBackoff(Constants.Standard.MAX_BACKOFF)
                .retryCost(Constants.Standard.RETRY_COST)
                .throttlingRetryCost(Constants.Standard.THROTTLING_RETRY_COST)
                .initialRetryTokens(Constants.Standard.INITIAL_RETRY_TOKENS)
                .maxScopes(Constants.Standard.MAX_SCOPES);
    }

    /**
     * Creates a new {@link StandardRetryStrategy} with default values for regular use cases.
     *
     * @return the created strategy.
     */
    public static StandardRetryStrategy create() {
        return builder().build();
    }

    /**
     * A builder for {@link StandardRetryStrategy}.
     */
    public static final class Builder extends BaseRetryStrategy.Builder {
        Builder() {
            super();
        }

        Builder(StandardRetryStrategy retryStrategy) {
            super(retryStrategy);
        }

        @Override
        public StandardRetryStrategy build() {
            return new StandardRetryStrategy(this);
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            setMaxAttempts(maxAttempts);
            return this;
        }

        /**
         * Set the token cost for non-throttling retries.
         *
         * @param retryCost the retry cost.
         * @return the builder.
         */
        public Builder retryCost(int retryCost) {
            setRetryCost(retryCost);
            return this;
        }

        /**
         * Set the token cost for throttling retries.
         *
         * @param throttlingRetryCost the throttling retry cost.
         * @return the builder.
         */
        public Builder throttlingRetryCost(int throttlingRetryCost) {
            setThrottlingRetryCost(throttlingRetryCost);
            return this;
        }

        /**
         * Set the base delay for exponential backoff.
         *
         * @param baseDelay the base delay.
         * @return the builder.
         */
        public Builder backoffBaseDelay(Duration baseDelay) {
            setBackoffBaseDelay(baseDelay);
            return this;
        }

        /**
         * Set the maximum backoff delay.
         *
         * @param maxBackoff the maximum backoff.
         * @return the builder.
         */
        public Builder backoffMaxBackoff(Duration maxBackoff) {
            setBackoffMaxDelay(maxBackoff);
            return this;
        }

        /**
         * Set the initial number of retry tokens per scope.
         *
         * @param initialRetryTokens the initial token count.
         * @return the builder.
         */
        public Builder initialRetryTokens(int initialRetryTokens) {
            setInitialRetryTokens(initialRetryTokens);
            return this;
        }

        /**
         * Set the maximum number of scopes to track.
         *
         * <p>When the limit is reached, the least recently used scope is evicted.
         *
         * @param maxScopes the maximum number of scopes.
         * @return the builder.
         */
        public Builder maxScopes(int maxScopes) {
            setMaxScopes(maxScopes);
            return this;
        }
    }
}
