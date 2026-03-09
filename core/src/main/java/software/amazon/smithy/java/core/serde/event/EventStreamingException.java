/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import java.util.Objects;

/**
 * Exception thrown upon receiving events of type :message-type exception.
 */
public final class EventStreamingException extends EventStreamingProtocolException {

    private final String errorCode;

    public EventStreamingException(String errorCode, String errorMessage) {
        super(Objects.requireNonNull(errorMessage, "errorMessage"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    /**
     * Returns the error code of the exception.
     *
     * @return the error code of the exception
     */
    public String getErrorCode() {
        return errorCode;
    }
}
