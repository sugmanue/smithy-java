/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

import software.amazon.smithy.java.core.schema.InputEventStreamingApiOperation;
import software.amazon.smithy.java.core.schema.OutputEventStreamingApiOperation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.event.EventDecoder;
import software.amazon.smithy.java.core.serde.event.EventDecoderFactory;
import software.amazon.smithy.java.core.serde.event.FrameDecoder;
import software.amazon.smithy.java.core.serde.event.FrameTransformer;

import java.util.function.Supplier;

public class AwsEventDecoderFactory<E extends SerializableStruct, IR extends SerializableStruct>
        implements EventDecoderFactory<AwsEventFrame> {

    private final Supplier<ShapeBuilder<IR>> initialResponseBuilder;
    private final Schema eventSchema;
    private final Codec codec;
    private final Supplier<ShapeBuilder<E>> eventBuilder;
    private final FrameTransformer<AwsEventFrame> transformer;

    private AwsEventDecoderFactory(
            Supplier<ShapeBuilder<IR>> initialResponseBuilder,
            Schema eventSchema,
            Codec codec,
            Supplier<ShapeBuilder<E>> eventBuilder,
            FrameTransformer<AwsEventFrame> transformer
    ) {
        this.initialResponseBuilder = initialResponseBuilder;
        this.eventSchema = eventSchema.isMember() ? eventSchema.memberTarget() : eventSchema;
        this.codec = codec;
        this.eventBuilder = eventBuilder;
        this.transformer = transformer;
    }

    private AwsEventDecoderFactory(
            Schema eventSchema,
            Codec codec,
            Supplier<ShapeBuilder<E>> eventBuilder,
            FrameTransformer<AwsEventFrame> transformer
    ) {
        this(AwsEventDecoderFactory::unsupportedInitialResponseBuilder, eventSchema, codec, eventBuilder, transformer);
    }

    public static <IE extends SerializableStruct> AwsEventDecoderFactory<IE, ?> forInputStream(
            InputEventStreamingApiOperation<?, ?, IE> operation,
            Codec codec,
            FrameTransformer<AwsEventFrame> transformer
    ) {
        return new AwsEventDecoderFactory<>(
                operation.inputStreamMember(),
                codec,
                operation.inputEventBuilderSupplier(),
                transformer);
    }

    public static <OE extends SerializableStruct> AwsEventDecoderFactory<OE, ?> forOutputStream(
            OutputEventStreamingApiOperation<?, ?, OE> operation,
            Codec codec,
            FrameTransformer<AwsEventFrame> transformer
    ) {
        return new AwsEventDecoderFactory<>(
                operation::outputBuilder,
                operation.outputStreamMember(),
                codec,
                operation.outputEventBuilderSupplier(),
                transformer);
    }

    @Override
    public EventDecoder<AwsEventFrame> newEventDecoder() {
        return new AwsEventShapeDecoder<>(initialResponseBuilder, eventBuilder, eventSchema, codec);
    }

    @Override
    public FrameDecoder<AwsEventFrame> newFrameDecoder() {
        return new AwsFrameDecoder(transformer);
    }

    static <T extends SerializableStruct> ShapeBuilder<T> unsupportedInitialResponseBuilder() {
        throw new IllegalStateException("Initial response builder is not configured.");
    }
}
