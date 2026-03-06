/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.document;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A document wrapper around an {@link EventStream} for streaming union members.
 */
record EventStreamDocument(Schema schema, EventStream<? extends SerializableStruct> eventStream) implements Document {

    @Override
    public ShapeType type() {
        return ShapeType.UNION;
    }

    @Override
    public EventStream<? extends SerializableStruct> asEventStream() {
        return eventStream;
    }

    @Override
    public Object asObject() {
        return eventStream;
    }

    @Override
    public void serialize(ShapeSerializer serializer) {
        serializer.writeEventStream(schema, eventStream);
    }

    @Override
    public void serializeContents(ShapeSerializer serializer) {
        serializer.writeEventStream(schema, eventStream);
    }
}
