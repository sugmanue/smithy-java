/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries.api;

import java.time.Duration;

/**
 * Exception thrown by {@link RetryStrategy} when a new token cannot be acquired.
 */
public final class TokenAcquisitionFailedException extends RuntimeException {
    private final transient RetryToken token;
    private final transient Duration delay;

    public TokenAcquisitionFailedException(String msg) {
        super(msg);
        token = null;
        delay = Duration.ZERO;
    }

    public TokenAcquisitionFailedException(String msg, Throwable cause) {
        super(msg, cause);
        token = null;
        delay = Duration.ZERO;
    }

    public TokenAcquisitionFailedException(String msg, RetryToken token, Throwable cause) {
        super(msg, cause);
        this.token = token;
        this.delay = Duration.ZERO;
    }

    public TokenAcquisitionFailedException(String msg, RetryToken token, Throwable cause, Duration delay) {
        super(msg, cause);
        this.token = token;
        this.delay = delay != null ? delay : Duration.ZERO;
    }

    /**
     * Returns the retry token that tracked the execution.
     * @return the retry token.
     */
    public RetryToken token() {
        return token;
    }

    /**
     * Returns the delay to wait before returning the error to the caller.
     * This is non-zero for long-polling operations when retry quota is exhausted.
     * @return the delay.
     */
    public Duration delay() {
        return delay;
    }
}
