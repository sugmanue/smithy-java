/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.java.retries.api.AcquireInitialTokenFlags;
import software.amazon.smithy.java.retries.api.RetryToken;

/**
 * Default implementation of {@link RetryToken}.
 */
final class DefaultRetryToken implements RetryToken {
    private final String scope;
    private final int flags;
    private final int attempt;
    private final int capacityAcquired;
    private final int capacityRemaining;
    private final List<Throwable> failures;

    private DefaultRetryToken(Builder builder) {
        this.scope = Objects.requireNonNull(builder.scope, "scope");
        this.flags = builder.flags;
        this.attempt = validateIsPositive(builder.attempt, "attempt");
        this.capacityAcquired = validateIsNotNegative(builder.capacityAcquired, "capacityAcquired");
        this.capacityRemaining = validateIsNotNegative(builder.capacityRemaining, "capacityRemaining");
        this.failures = builder.failures == null ? List.of() : List.copyOf(builder.failures);
    }

    /**
     * Returns the latest attempt count.
     */
    int attempt() {
        return attempt;
    }

    /**
     * Returns the token scope.
     */
    String scope() {
        return scope;
    }

    /**
     * Returns the flags for this token.
     */
    int flags() {
        return flags;
    }

    /**
     * Returns true if the token is configured for a long polling operation.
     */
    boolean isLongPolling() {
        return AcquireInitialTokenFlags.isLongPolling(flags);
    }

    /**
     * Returns the latest capacity acquired from the token bucket.
     */
    int capacityAcquired() {
        return capacityAcquired;
    }

    /**
     * Returns the capacity remaining in the token bucket when the last acquire request was done.
     */
    int capacityRemaining() {
        return capacityRemaining;
    }

    /**
     * Creates a new builder to mutate the current instance.
     */
    Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return "DefaultRetryToken{" +
                "scope='" + scope + '\'' +
                ", attempt=" + attempt +
                ", capacityAcquired=" + capacityAcquired +
                ", capacityRemaining=" + capacityRemaining +
                ", failures=" + failures +
                '}';
    }

    /**
     * Returns a new builder to create new instances of the {@link DefaultRetryToken} class.
     */
    static Builder builder() {
        return new Builder();
    }

    static int validateIsPositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static int validateIsNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return value;
    }

    /**
     * A builder class to create {@link DefaultRetryToken} instances or to mutate them.
     */
    static final class Builder {
        private static final int MAX_FAILURES = 10;
        private String scope;
        private int flags;
        private int attempt = 1;
        private int capacityAcquired = 0;
        private int capacityRemaining = 0;
        private ArrayDeque<Throwable> failures;

        Builder() {}

        Builder(DefaultRetryToken token) {
            this.scope = token.scope;
            this.attempt = token.attempt;
            this.capacityAcquired = token.capacityAcquired;
            this.capacityRemaining = token.capacityRemaining;
            this.flags = token.flags;
            this.failures = token.failures.isEmpty() ? null : new ArrayDeque<>(token.failures);
        }

        /**
         * Sets the scope of the retry token.
         */
        Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets the flags for this token.
         */
        Builder flags(int flags) {
            this.flags = flags;
            return this;
        }

        /**
         * Increments the current attempt count.
         */
        Builder increaseAttempt() {
            ++this.attempt;
            return this;
        }

        /**
         * Sets the capacity acquired from the token bucket.
         */
        Builder capacityAcquired(int capacityAcquired) {
            this.capacityAcquired = capacityAcquired;
            return this;
        }

        /**
         * Sets the capacity remaining in the token bucket after the last acquire.
         */
        Builder capacityRemaining(int capacityRemaining) {
            this.capacityRemaining = capacityRemaining;
            return this;
        }

        /**
         * Adds a {@link Throwable} to the retry-token.
         *
         * <p>Only the most recent {@value MAX_FAILURES} failures are retained.
         */
        Builder addFailure(Throwable failure) {
            Objects.requireNonNull(failure, "failure");
            if (failures == null) {
                failures = new ArrayDeque<>();
            } else if (this.failures.size() >= MAX_FAILURES) {
                this.failures.pollFirst();
            }
            this.failures.addLast(failure);
            return this;
        }

        /**
         * Creates a new {@link DefaultRetryToken} with the configured values.
         */
        DefaultRetryToken build() {
            return new DefaultRetryToken(this);
        }
    }
}
