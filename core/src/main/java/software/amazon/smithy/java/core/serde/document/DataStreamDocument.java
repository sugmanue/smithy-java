/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.document;

import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.io.datastream.DataStream;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * A document wrapper around a {@link DataStream} for streaming blob members.
 */
record DataStreamDocument(Schema schema, DataStream dataStream) implements Document {

    @Override
    public ShapeType type() {
        return ShapeType.BLOB;
    }

    @Override
    public DataStream asDataStream() {
        return dataStream;
    }

    @Override
    public Object asObject() {
        return dataStream;
    }

    @Override
    public void serialize(ShapeSerializer serializer) {
        serializer.writeDataStream(schema, dataStream);
    }

    @Override
    public void serializeContents(ShapeSerializer serializer) {
        serializer.writeDataStream(schema, dataStream);
    }
}
