/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;
import software.amazon.smithy.java.core.error.ModeledException;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaUtils;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.SpecificShapeSerializer;
import software.amazon.smithy.java.core.serde.event.EventEncoder;
import software.amazon.smithy.java.core.serde.event.EventStreamingException;
import software.amazon.smithy.model.shapes.ShapeId;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AwsEventShapeEncoder implements EventEncoder<AwsEventFrame> {

    private final Codec codec;
    private final String payloadMediaType;
    private final Set<String> possibleTypes;
    private final Map<ShapeId, Schema> possibleExceptions;
    private final Function<Throwable, EventStreamingException> exceptionHandler;

    public AwsEventShapeEncoder(
            Schema eventSchema,
            Codec codec,
            String payloadMediaType,
            Function<Throwable, EventStreamingException> exceptionHandler
    ) {
        this.codec = codec;
        this.payloadMediaType = payloadMediaType;
        this.possibleTypes = possibleTypes(eventSchema);
        this.possibleExceptions = possibleExceptions(eventSchema);
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public AwsEventFrame encode(SerializableStruct item) {
        var typeHolder = new AtomicReference<String>();
        var payload = encodeInput(item, typeHolder);
        var headers = Map.of(
                ":message-type", HeaderValue.fromString("event"),
                ":event-type", HeaderValue.fromString(typeHolder.get()),
                ":content-type", HeaderValue.fromString(payloadMediaType));
        return new AwsEventFrame(new Message(headers, payload));
    }

    private byte[] encodeInput(SerializableStruct item, AtomicReference<String> typeHolder) {
        if (isInitialRequest(item.schema())) {
            // The initial request is serialized fully instead of just a single member as for events.
            typeHolder.compareAndSet(null, "initial-request");
            var os = new ByteArrayOutputStream();
            try (var baseSerializer = codec.createSerializer(os)) {
                SchemaUtils.withFilteredMembers(item.schema(), item, this::excludeEventStreamMember)
                        .serialize(baseSerializer);
            }
            return os.toByteArray();
        }
        var os = new ByteArrayOutputStream();
        try (var baseSerializer = codec.createSerializer(os)) {
            var serializer = new SpecificShapeSerializer() {
                @Override
                public void writeStruct(Schema schema, SerializableStruct struct) {
                    if (possibleTypes.contains(schema.memberName())) {
                        typeHolder.compareAndSet(null, schema.memberName());
                    }
                    baseSerializer.writeStruct(schema, struct);
                }
            };
            item.serializeMembers(serializer);
        }
        return os.toByteArray();
    }

    private boolean isInitialRequest(Schema schema) {
        for (var member : schema.members()) {
            if (isEventStreamMember(member)) {
                return true;
            }
        }
        return false;
    }

    private boolean excludeEventStreamMember(Schema schema) {
        return !isEventStreamMember(schema);
    }

    private boolean isEventStreamMember(Schema schema) {
        if (schema.isMember() && schema.memberTarget().hasTrait(TraitKey.STREAMING_TRAIT)) {
            return true;
        }
        return false;
    }

    @Override
    public AwsEventFrame encodeFailure(Throwable exception) {
        AwsEventFrame frame;
        Schema exceptionSchema;
        if (exception instanceof ModeledException me
                && (exceptionSchema = possibleExceptions.get(me.schema().id())) != null
        ) {
            var headers = Map.of(
                    ":message-type", HeaderValue.fromString("exception"),
                    ":exception-type", HeaderValue.fromString(exceptionSchema.memberName()),
                    ":content-type", HeaderValue.fromString(payloadMediaType));
            var payload = codec.serialize(me);
            var bytes = new byte[payload.remaining()];
            payload.get(bytes);
            frame = new AwsEventFrame(new Message(headers, bytes));
        } else {
            EventStreamingException es = exceptionHandler.apply(exception);
            var headers = Map.of(
                    ":message-type", HeaderValue.fromString("error"),
                    ":error-code", HeaderValue.fromString(es.getErrorCode()),
                    ":error-message", HeaderValue.fromString(es.getMessage()));
            frame = new AwsEventFrame(new Message(headers, new byte[0]));
        }
        return frame;

    }

    static Set<String> possibleTypes(Schema eventSchema) {
        var result = new HashSet<String>();
        for (var memberSchema : eventSchema.members()) {
            String memberName = memberSchema.memberName();
            result.add(memberName);
        }
        return Collections.unmodifiableSet(result);
    }

    static Map<ShapeId, Schema>  possibleExceptions(Schema eventSchema) {
        var result = new HashMap<ShapeId, Schema>();
        for (var memberSchema : eventSchema.members()) {
            if (memberSchema.hasTrait(TraitKey.ERROR_TRAIT)) {
                if (result.put(memberSchema.memberTarget().id(), memberSchema) != null) {
                    throw new IllegalStateException("Duplicate key");
                }
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
