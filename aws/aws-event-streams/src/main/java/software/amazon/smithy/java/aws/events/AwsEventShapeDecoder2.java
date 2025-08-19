/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

import software.amazon.eventstream.Message;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.SpecificShapeDeserializer;
import software.amazon.smithy.java.core.serde.event.EventDecoder;

import java.util.concurrent.Flow;
import java.util.function.Supplier;

public final class AwsEventShapeDecoder2<E extends SerializableStruct, IR extends SerializableStruct>
        implements EventDecoder<AwsEventFrame> {

    private final Supplier<ShapeBuilder<IR>> responseBuilder;
    private final Supplier<ShapeBuilder<E>> eventBuilder;
    private final Schema eventSchema;
    private final Codec codec;
    private volatile Flow.Publisher<SerializableStruct> publisher;

    public AwsEventShapeDecoder2(
            Supplier<ShapeBuilder<IR>> responseBuilder,
            Supplier<ShapeBuilder<E>> eventBuilder,
            Schema eventSchema,
            Codec codec
    ) {
        this.responseBuilder = responseBuilder;
        this.eventBuilder = eventBuilder;
        this.eventSchema = eventSchema;
        this.codec = codec;
    }

    @Override
    public SerializableStruct decode(AwsEventFrame frame) {
        var message = frame.unwrap();
        var eventType = getEventType(message);
        if ("initial-response".equals(eventType)) {
            return decodeInitialResponse(frame);
        }
        return decodeEvent(frame);
    }

    @Override
    public void onPrepare(Flow.Publisher<SerializableStruct> publisher) {
        this.publisher = publisher;
    }

    public E decodeEvent(AwsEventFrame frame) {
        var message = frame.unwrap();
        var eventType = getEventType(message);
        var memberSchema = eventSchema.member(eventType);
        if (memberSchema == null) {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
        var builder = eventBuilder.get();
        var codecDeserializer = codec.createDeserializer(message.getPayload());
        var eventDeserializer = new AwsEventDeserializer(memberSchema, codecDeserializer);
        E result = builder.deserialize(eventDeserializer).build();
        return result;
    }

    public IR decodeInitialResponse(AwsEventFrame frame) {
        var message = frame.unwrap();
        var builder = responseBuilder.get();
        var codecDeserializer = codec.createDeserializer(message.getPayload());
        builder.deserialize(codecDeserializer);
        var publisherMember = getPublisherMember(builder.schema());
        var responseDeserializer = new EventStreamDeserializer(publisherMember, publisher);
        builder.deserialize(responseDeserializer);
        return builder.build();
    }

    private Schema getPublisherMember(Schema schema) {
        for (var member : schema.members()) {
            if (member.memberTarget().hasTrait(TraitKey.STREAMING_TRAIT)) {
                return member;
            }
        }
        throw new IllegalArgumentException("cannot find streaming member");
    }

    private String getEventType(Message message) {
        return message.getHeaders().get(":event-type").getString();
    }

    private String getMessageType(Message message) {
        return message.getHeaders().get(":message-type").getString();
    }

    static class EventStreamDeserializer extends SpecificShapeDeserializer {
        private final Schema publisherMember;
        private final Flow.Publisher<? extends SerializableStruct> publisher;

        EventStreamDeserializer(Schema publisherMember, Flow.Publisher<? extends SerializableStruct> publisher) {
            this.publisherMember = publisherMember;
            this.publisher = publisher;
        }

        @Override
        public Flow.Publisher<? extends SerializableStruct> readEventStream(Schema schema) {
            return publisher;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, ShapeDeserializer.StructMemberConsumer<T> consumer) {
            consumer.accept(state, publisherMember, this);
        }
    }
}
