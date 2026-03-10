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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public sealed interface EventStreamWithError extends SerializableStruct {
    Schema $SCHEMA = Schemas.EVENT_STREAM_WITH_ERROR;

    ShapeId $ID = $SCHEMA.id();

    <T> T getValue();

    @Override
    default Schema schema() {
        return $SCHEMA;
    }

    @Override
    default <T> T getMemberValue(Schema member) {
        return SchemaUtils.validateMemberInSchema($SCHEMA, member, getValue());
    }

    @SmithyGenerated
    record ModeledErrorMemberMember(MyError modeledErrorMember) implements EventStreamWithError {
        private static final Schema $SCHEMA_MODELED_ERROR_MEMBER = $SCHEMA.member("modeledErrorMember");
        public ModeledErrorMemberMember {
            Objects.requireNonNull(modeledErrorMember, "Union value cannot be null");
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_MODELED_ERROR_MEMBER, modeledErrorMember);
        }

        @Override
        @SuppressWarnings("unchecked")
        public MyError getValue() {
            return modeledErrorMember;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    @SmithyGenerated
    record StringMemberMember(StringEvent stringMember) implements EventStreamWithError {
        private static final Schema $SCHEMA_STRING_MEMBER = $SCHEMA.member("stringMember");
        public StringMemberMember {
            Objects.requireNonNull(stringMember, "Union value cannot be null");
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {
            serializer.writeStruct($SCHEMA_STRING_MEMBER, stringMember);
        }

        @Override
        @SuppressWarnings("unchecked")
        public StringEvent getValue() {
            return stringMember;
        }

        @Override
        public String toString() {
            return ToStringSerializer.serialize(this);
        }

    }

    record $Unknown(String memberName) implements EventStreamWithError {
        @Override
        public void serialize(ShapeSerializer serializer) {
            throw new UnsupportedOperationException("Cannot serialize union with unknown member " + this.memberName);
        }

        @Override
        public void serializeMembers(ShapeSerializer serializer) {}

        @Override
        @SuppressWarnings("unchecked")
        public String getValue() {
            return memberName;
        }

        private record $Hidden() implements EventStreamWithError {
            @Override
            public void serializeMembers(ShapeSerializer serializer) {}

            @Override
            @SuppressWarnings("unchecked")
            public <T> T getValue() {
                return null;
            }
        }
    }

    interface BuildStage {
        EventStreamWithError build();
    }

    /**
     * @return returns a new Builder.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link EventStreamWithError}.
     */
    final class Builder implements ShapeBuilder<EventStreamWithError>, BuildStage {
        private EventStreamWithError value;

        private Builder() {}

        @Override
        public Schema schema() {
            return $SCHEMA;
        }

        public BuildStage modeledErrorMember(MyError value) {
            return setValue(new ModeledErrorMemberMember(value));
        }

        public BuildStage stringMember(StringEvent value) {
            return setValue(new StringMemberMember(value));
        }

        public BuildStage $unknownMember(String memberName) {
            return setValue(new $Unknown(memberName));
        }

        private BuildStage setValue(EventStreamWithError value) {
            if (this.value != null) {
                throw new IllegalArgumentException("Only one value may be set for unions");
            }
            this.value = value;
            return this;
        }

        @Override
        public EventStreamWithError build() {
            return Objects.requireNonNull(value, "no union value set");
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setMemberValue(Schema member, Object value) {
            switch (member.memberIndex()) {
                case 0 -> modeledErrorMember((MyError) SchemaUtils
                        .validateSameMember(ModeledErrorMemberMember.$SCHEMA_MODELED_ERROR_MEMBER, member, value));
                case 1 -> stringMember((StringEvent) SchemaUtils
                        .validateSameMember(StringMemberMember.$SCHEMA_STRING_MEMBER, member, value));
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
                    case 0 -> builder.modeledErrorMember(MyError.builder().deserializeMember(de, member).build());
                    case 1 -> builder.stringMember(StringEvent.builder().deserializeMember(de, member).build());
                    default -> throw new IllegalArgumentException("Unexpected member: " + member.memberName());
                }
            }

            @Override
            public void unknownMember(Builder builder, String memberName) {
                builder.$unknownMember(memberName);
            }
        }
    }
}
