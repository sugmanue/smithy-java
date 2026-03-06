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
public final class TestOperationWithExceptionOutput implements SerializableStruct {

    public static final Schema $SCHEMA = Schemas.TEST_OPERATION_WITH_EXCEPTION_OUTPUT;
    private static final Schema $SCHEMA_OUTPUT_STREAM = $SCHEMA.member("outputStream");

    public static final ShapeId $ID = $SCHEMA.id();

    private final transient EventStream<EventStreamWithError> outputStream;

    private TestOperationWithExceptionOutput(Builder builder) {
        this.outputStream = builder.outputStream;
    }

    public EventStream<EventStreamWithError> getOutputStream() {
        return outputStream;
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
        TestOperationWithExceptionOutput that = (TestOperationWithExceptionOutput) other;
        return Objects.equals(this.outputStream, that.outputStream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputStream);
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public void serializeMembers(ShapeSerializer serializer) {
        if (outputStream != null) {
            serializer.writeEventStream($SCHEMA_OUTPUT_STREAM, outputStream);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMemberValue(Schema member) {
        return switch (member.memberIndex()) {
            case 0 -> (T) SchemaUtils.validateSameMember($SCHEMA_OUTPUT_STREAM, member, outputStream);
            default -> throw new IllegalArgumentException("Attempted to get non-existent member: " + member.id());
        };
    }

    /**
     * Create a new builder containing all the current property values of this object.
     *
     * <p><strong>Note:</strong> This method performs only a shallow copy of the original properties.
     *
     * @return a builder for {@link TestOperationWithExceptionOutput}.
     */
    public Builder toBuilder() {
        var builder = new Builder();
        builder.outputStream(this.outputStream);
        return builder;
    }

    /**
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TestOperationWithExceptionOutput}.
     */
    public static final class Builder implements ShapeBuilder<TestOperationWithExceptionOutput> {
        private EventStream<EventStreamWithError> outputStream;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        /**
         * @return this builder.
         */
        public Builder outputStream(EventStream<EventStreamWithError> outputStream) {
            this.outputStream = outputStream;
            return this;
        }

        @Override
        public TestOperationWithExceptionOutput build() {
            return new TestOperationWithExceptionOutput(this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> outputStream((EventStream<EventStreamWithError>) SchemaUtils
                        .validateSameMember($SCHEMA_OUTPUT_STREAM, member, value));
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
                    case 0 -> builder.outputStream((EventStream<EventStreamWithError>) de.readEventStream(member));
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }
        }
    }
}
