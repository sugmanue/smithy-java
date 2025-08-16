/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.io.datastream.DataStream;

public class DelegatingShapeDeserializer implements ShapeDeserializer {
    private final ShapeDeserializer delegate;

    public DelegatingShapeDeserializer(ShapeDeserializer delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean readBoolean(Schema schema) {
        return delegate.readBoolean(schema);
    }

    @Override
    public ByteBuffer readBlob(Schema schema) {
        return delegate.readBlob(schema);
    }

    @Override
    public byte readByte(Schema schema) {
        return delegate.readByte(schema);
    }

    @Override
    public short readShort(Schema schema) {
        return delegate.readShort(schema);
    }

    @Override
    public int readInteger(Schema schema) {
        return delegate.readInteger(schema);
    }

    @Override
    public long readLong(Schema schema) {
        return delegate.readLong(schema);
    }

    @Override
    public float readFloat(Schema schema) {
        return delegate.readFloat(schema);
    }

    @Override
    public double readDouble(Schema schema) {
        return delegate.readDouble(schema);
    }

    @Override
    public BigInteger readBigInteger(Schema schema) {
        return delegate.readBigInteger(schema);
    }

    @Override
    public BigDecimal readBigDecimal(Schema schema) {
        return delegate.readBigDecimal(schema);
    }

    @Override
    public String readString(Schema schema) {
        return delegate.readString(schema);
    }

    @Override
    public Document readDocument() {
        return delegate.readDocument();
    }

    @Override
    public Instant readTimestamp(Schema schema) {
        return delegate.readTimestamp(schema);
    }

    @Override
    public <T> void readStruct(Schema schema, T state, StructMemberConsumer<T> consumer) {
        delegate.readStruct(schema, state, consumer);
    }

    @Override
    public <T> void readList(Schema schema, T state, ListMemberConsumer<T> consumer) {
        delegate.readList(schema, state, consumer);
    }

    @Override
    public int containerSize() {
        return delegate.containerSize();
    }

    @Override
    public <T> void readStringMap(Schema schema, T state, MapMemberConsumer<String, T> consumer) {
        delegate.readStringMap(schema, state, consumer);
    }

    @Override
    public boolean isNull() {
        return delegate.isNull();
    }

    @Override
    public <T> T readNull() {
        return delegate.readNull();
    }

    @Override
    public DataStream readDataStream(Schema schema) {
        return delegate.readDataStream(schema);
    }

    @Override
    public Flow.Publisher<? extends SerializableStruct> readEventStream(Schema schema) {
        return delegate.readEventStream(schema);
    }
}
