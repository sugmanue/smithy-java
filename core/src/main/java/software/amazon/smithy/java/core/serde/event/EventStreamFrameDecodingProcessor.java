/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;
import java.util.stream.Stream;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.BufferingFlatMapProcessor;

public final class EventStreamFrameDecodingProcessor<F extends Frame<?>>
        extends BufferingFlatMapProcessor<ByteBuffer, SerializableStruct> {
    private final FrameDecoder<F> frameDecoder;
    private final EventDecoder<F> eventDecoder;

    EventStreamFrameDecodingProcessor(
            Flow.Publisher<ByteBuffer> publisher,
            FrameDecoder<F> frameDecoder,
            EventDecoder<F> eventDecoder
    ) {
        super(publisher);
        this.frameDecoder = frameDecoder;
        this.eventDecoder = eventDecoder;
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

    public void prepare() {
        frameDecoder.onPrepare(this);
        eventDecoder.onPrepare(this);
    }

    @Override
    protected Stream<SerializableStruct> map(ByteBuffer item) {
        return frameDecoder.decode(item).stream().map(eventDecoder::decode);
    }
}
