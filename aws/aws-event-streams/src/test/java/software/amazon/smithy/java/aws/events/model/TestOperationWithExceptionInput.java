/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events.model;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;
import software.amazon.smithy.java.core.serde.ToStringSerializer;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class TestOperationWithExceptionInput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TEST_OPERATION_WITH_EXCEPTION_INPUT;
    private static final Schema $SCHEMA_STREAM = $SCHEMA.member("stream");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient EventStream<TestEventStream> stream;

    private TestOperationWithExceptionInput(Builder builder) {
        this.stream = builder.stream;
    }

    public EventStream<TestEventStream> getStream() {
        return stream;
    }

    @Override
    public String toString() {
        return ToStringSerializer.serialize(this);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        TestOperationWithExceptionInput that = (TestOperationWithExceptionInput) other;
        return Objects.equals(this.stream, that.stream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stream);
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (stream != null) {
            serializer.writeEventStream($SCHEMA_STREAM, stream);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_STREAM, member, stream);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TestOperationWithExceptionInput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.stream(this.stream);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TestOperationWithExceptionInput}.
     */
    public static final class Builder implements ShapeBuilder<TestOperationWithExceptionInput> {
        private EventStream<TestEventStream> stream;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder stream(EventStream<TestEventStream> stream) {
            this.stream = stream;
            return this;
        }

        @Override
        public TestOperationWithExceptionInput build() {
            return new TestOperationWithExceptionInput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> stream(
                        (EventStream<TestEventStream>) SchemaUtils.validateSameMember($SCHEMA_STREAM, member, value));
                default -> ShapeBuilder.super.setMemberValue(member, value);
            }
        }

        @Override
        public Builder deserialize(ShapeDeserializer decoder) {
            decoder.readStruct($SCHEMA, this, $InnerDeserializer.INSTANCE);
            return this;
        }

        @Override
        public Builder deserializeMember(ShapeDeserializer decoder, Schema schema) {
            decoder.readStruct(schema.assertMemberTargetIs($SCHEMA), this, $InnerDeserializer.INSTANCE);
            return this;
        }

        private static final class $InnerDeserializer implements ShapeDeserializer.StructMemberConsumer<Builder> {
            private static final $InnerDeserializer INSTANCE = new $InnerDeserializer();

            @Override
            @SuppressWarnings("unchecked")
            public void accept(Builder builder, Schema member, ShapeDeserializer de) {
                switch (member.memberIndex()) {
                    case 0 -> builder.stream((EventStream<TestEventStream>) de.readEventStream(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
