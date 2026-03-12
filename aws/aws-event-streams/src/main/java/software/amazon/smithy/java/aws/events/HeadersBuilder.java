/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import software.amazon.eventstream.HeaderValue;

/**
 * Util class to build headers for AWS events.
 */
final class HeadersBuilder {

    private final Map<String, HeaderValue> headers = new HashMap<>();

    HeadersBuilder() {}

    /**
     * Returns a headers builder for messages of type event.
     */
    static HeadersBuilder forEvent() {
        return new HeadersBuilder()
                .messageType(MessageType.EVENT);
    }

    /**
     * Returns a headers builder for messages of type error.
     */
    static HeadersBuilder forError() {
        return new HeadersBuilder()
                .messageType(MessageType.ERROR);
    }

    /**
     * Returns a headers builder for messages of type exception.
     */
    static HeadersBuilder forException() {
        return new HeadersBuilder()
                .messageType(MessageType.EXCEPTION);
    }

    public HeadersBuilder messageType(MessageType messageType) {
        headers.put(":message-type", HeaderValue.fromString(messageType.toString()));
        return this;
    }

    /**
     * Sets the `:content-type` header.
     */
    public HeadersBuilder contentType(String contentType) {
        headers.put(":content-type", HeaderValue.fromString(contentType));
        return this;
    }

    /**
     * Sets the `:event-type` header.
     */
    public HeadersBuilder eventType(String eventType) {
        headers.put(":event-type", HeaderValue.fromString(eventType));
        return this;
    }

    /**
     * Sets the `:exception-type` header.
     */
    public HeadersBuilder exceptionType(String eventType) {
        headers.put(":exception-type", HeaderValue.fromString(eventType));
        return this;
    }

    /**
     * Sets the `:error-code` header.
     */
    public HeadersBuilder errorCode(String errorCode) {
        headers.put(":error-code", HeaderValue.fromString(errorCode));
        return this;
    }

    /**
     * Sets the `:error-message` header.
     */
    public HeadersBuilder errorMessage(String errorMessage) {
        headers.put(":error-message", HeaderValue.fromString(errorMessage));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, String value) {
        headers.put(name, HeaderValue.fromString(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, int value) {
        headers.put(name, HeaderValue.fromInteger(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, long value) {
        headers.put(name, HeaderValue.fromLong(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, boolean value) {
        headers.put(name, HeaderValue.fromBoolean(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, short value) {
        headers.put(name, HeaderValue.fromShort(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, byte value) {
        headers.put(name, HeaderValue.fromByte(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, ByteBuffer value) {
        headers.put(name, HeaderValue.fromByteBuffer(value));
        return this;
    }

    /**
     * Adds the header with the given name and value.
     */
    public HeadersBuilder put(String name, Instant value) {
        headers.put(name, HeaderValue.fromTimestamp(value));
        return this;
    }

    /**
     * Returns the map with the header values.
     */
    public Map<String, HeaderValue> build() {
        return headers;
    }

    enum MessageType {
        EVENT("event"),
        ERROR("error"),
        EXCEPTION("exception");

        private final String value;

        MessageType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
