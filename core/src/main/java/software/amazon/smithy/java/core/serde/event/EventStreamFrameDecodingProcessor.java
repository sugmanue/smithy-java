/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.BufferingFlatMapProcessor;

public final class EventStreamFrameDecodingProcessor<F extends Frame<?>>
        extends BufferingFlatMapProcessor<ByteBuffer, SerializableStruct> {
    private final FrameDecoder<F> decoder;
    private final EventDecoder<F> eventDecoder;
    private final InitialResponseDecoder<F> initialResponseDecoder;
    private final AtomicBoolean initialResponseConsumed = new AtomicBoolean();

    public EventStreamFrameDecodingProcessor(
            Flow.Publisher<ByteBuffer> publisher,
            FrameDecoder<F> decoder,
            EventDecoder<F> eventDecoder
    ) {
        super(publisher);
        this.decoder = decoder;
        this.eventDecoder = eventDecoder;
        this.initialResponseDecoder = null;
    }

    public EventStreamFrameDecodingProcessor(
            Flow.Publisher<ByteBuffer> publisher,
            FrameDecoder<F> decoder,
            EventDecoder<F> eventDecoder,
            InitialResponseDecoder<F> initialResponseDecoder
    ) {
        super(publisher);
        this.decoder = decoder;
        this.eventDecoder = eventDecoder;
        this.initialResponseDecoder = initialResponseDecoder;
    }

    public static <F extends Frame<?>> EventStreamFrameDecodingProcessor<F> create(
            Flow.Publisher<ByteBuffer> publisher,
            EventDecoderFactory<F> eventDecoderFactory
    ) {
        return new EventStreamFrameDecodingProcessor<>(
                publisher,
                eventDecoderFactory.newFrameDecoder(),
                eventDecoderFactory.newEventDecoder());
    }

    public static <F extends Frame<?>> EventStreamFrameDecodingProcessor<F> create(
            Flow.Publisher<ByteBuffer> publisher,
            EventDecoderFactory<F> eventDecoderFactory,
            InitialResponseDecoder<F> initialResponseDecoder
    ) {
        return new EventStreamFrameDecodingProcessor<>(
                publisher,
                eventDecoderFactory.newFrameDecoder(),
                eventDecoderFactory.newEventDecoder(),
                initialResponseDecoder);
    }

    @Override
    protected Stream<SerializableStruct> map(ByteBuffer item) {
        var frames = decoder.decode(item);
        if (!frames.isEmpty() && initialResponseConsumed.compareAndSet(false, true)) {
            var initialResponse = initialResponseDecoder.decode(frames.get(0), this);
            return Stream.concat(Stream.of(initialResponse), frames.stream().skip(1).map(eventDecoder::decode));
        }
        return frames.stream().map(eventDecoder::decode);
    }
}
