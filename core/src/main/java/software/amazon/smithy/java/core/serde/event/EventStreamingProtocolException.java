/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

/**
 * Generic exception thrown in event streaming.
 */
public class EventStreamingProtocolException extends RuntimeException {
    /**
     * Creates a new exception.
     *
     * @param message the message.
     */
    public EventStreamingProtocolException(String message) {
        super(message);
    }
}
