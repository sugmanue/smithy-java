/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries.api;

/**
 * Flags that can be set on an {@link AcquireInitialTokenRequest} to modify retry behavior.
 *
 * <p>Flags are combined using bitwise OR, e.g. {@code AcquireInitialTokenFlags.IS_LONG_POLLING | otherFlag}.
 */
public final class AcquireInitialTokenFlags {

    /**
     * Indicates that the operation is long polling. Retry strategies can use this to allow retries even when the
     * token bucket would otherwise reject the attempt.
     */
    public static final int IS_LONG_POLLING = 1;

    /**
     * Returns {@code true} if the {@link #IS_LONG_POLLING} flag is set.
     */
    public static boolean isLongPolling(int flags) {
        return (flags & IS_LONG_POLLING) == IS_LONG_POLLING;
    }

    private AcquireInitialTokenFlags() {}
}
