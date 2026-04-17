/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.time.Duration;
import software.amazon.smithy.java.logging.InternalLogger;
import software.amazon.smithy.java.retries.api.AcquireInitialTokenRequest;
import software.amazon.smithy.java.retries.api.RefreshRetryTokenRequest;

/**
 * An adaptive retry strategy that dynamically adjusts retry behavior based on throttling responses.
 *
 * <p>This strategy extends the standard retry behavior with client-side rate limiting. It uses a CUBIC-based
 * algorithm to estimate the maximum safe send rate and throttles requests when the service is under pressure.
 *
 * @see StandardRetryStrategy
 */
public final class AdaptiveRetryStrategy extends BaseRetryStrategy {
    private static final InternalLogger LOGGER = InternalLogger.getLogger(AdaptiveRetryStrategy.class);
    private final RateLimiterTokenBucketStore rateLimiterTokenBucketStore;

    AdaptiveRetryStrategy(Builder builder) {
        super(LOGGER, builder);
        this.rateLimiterTokenBucketStore = new RateLimiterTokenBucketStore(builder.maxScopes);
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    protected Duration computeInitialBackoff(AcquireInitialTokenRequest request) {
        var bucket = rateLimiterTokenBucketStore.tokenBucketForScope(request.scope());
        return bucket.acquirePermit();
    }

    @Override
    protected Duration computeBackoff(RefreshRetryTokenRequest request, DefaultRetryToken token) {
        var backoff = super.computeBackoff(request, token);
        var bucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
        return backoff.plus(bucket.acquirePermit());
    }

    @Override
    protected void updateStateForRetry(RefreshRetryTokenRequest request) {
        if (treatAsThrottling(request.failure())) {
            var token = asDefaultRetryToken(request.token());
            var bucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
            bucket.updateRateAfterThrottling();
        }
    }

    @Override
    protected void updateStateForSuccess(DefaultRetryToken token) {
        var bucket = rateLimiterTokenBucketStore.tokenBucketForScope(token.scope());
        bucket.updateRateAfterSuccess();
    }

    /**
     * Creates a new {@link Builder} with default configuration.
     *
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder()
                .maxAttempts(Constants.Adaptive.MAX_ATTEMPTS)
                .backoffBaseDelay(Constants.Adaptive.BASE_DELAY)
                .backoffMaxBackoff(Constants.Adaptive.MAX_BACKOFF)
                .retryCost(Constants.Adaptive.RETRY_COST)
                .throttlingRetryCost(Constants.Adaptive.THROTTLING_RETRY_COST)
                .initialRetryTokens(Constants.Adaptive.INITIAL_RETRY_TOKENS)
                .maxScopes(Constants.Adaptive.MAX_SCOPES);
    }

    /**
     * Creates a new {@link AdaptiveRetryStrategy} with default values.
     *
     * @return the created strategy.
     */
    public static AdaptiveRetryStrategy create() {
        return builder().build();
    }

    /**
     * A builder for {@link AdaptiveRetryStrategy}.
     */
    public static final class Builder extends BaseRetryStrategy.Builder {

        Builder() {}

        Builder(AdaptiveRetryStrategy strategy) {
            super(strategy);
        }

        @Override
        public AdaptiveRetryStrategy build() {
            return new AdaptiveRetryStrategy(this);
        }

        @Override
        public Builder maxAttempts(int maxAttempts) {
            setMaxAttempts(maxAttempts);
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
