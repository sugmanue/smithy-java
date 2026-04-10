/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries.api;

/**
 * An object that supports exclusive ownership by a single caller.
 *
 * <p>Some objects maintain internal state that should not be shared across multiple owners. For example, a
 * {@link RetryStrategy} tracks token bucket capacity and rate limiter state per scope. Sharing a single instance
 * across multiple clients causes one client's failures to affect another's retry behavior.
 *
 * <p>Callers should invoke {@link #claim(Object)} during construction to assert ownership. The first call succeeds;
 * subsequent calls throw {@link IllegalStateException}.
 *
 * <pre>{@code
 * if (retryStrategy instanceof Claimable c) {
 *     c.claim(this);
 * }
 * }</pre>
 *
 * <p>If a claimed object needs to be reused, callers should create a new instance via
 * {@link RetryStrategy#toBuilder()} instead of sharing the original.
 */
public interface Claimable {

    /**
     * Claims exclusive ownership of this object.
     *
     * <p>The first call to this method establishes exclusive ownership. Any subsequent call, regardless of the caller,
     * throws {@link IllegalStateException}.
     *
     * @param owner The object claiming ownership, typically {@code this} from the caller's constructor.
     * @throws NullPointerException  If {@code owner} is null.
     * @throws IllegalStateException If this object has already been claimed.
     */
    void claim(Object owner);
}
