/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import java.util.Objects;

/**
 * Wraps a factory with a frame processor.
 *
 * @param <F> the type of the frame
 */
final class ProcessingEventEncoderFactory<F extends Frame<?>> implements EventEncoderFactory<F> {
    private final EventEncoderFactory<F> eventDecoderFactory;
    private final FrameProcessor<F> frameProcessor;

    ProcessingEventEncoderFactory(EventEncoderFactory<F> eventDecoderFactory, FrameProcessor<F> frameProcessor) {
        this.eventDecoderFactory = Objects.requireNonNull(eventDecoderFactory, "eventDecoderFactory");
        this.frameProcessor = Objects.requireNonNull(frameProcessor, "frameProcessor");
    }

    @Override
    public EventEncoder<F> newEventEncoder() {
        var eventEncoder = eventDecoderFactory.newEventEncoder();
        return new ProcessingEventEncoder<>(eventEncoder, frameProcessor);
    }

    @Override
    public FrameEncoder<F> newFrameEncoder() {
        return eventDecoderFactory.newFrameEncoder();
    }

    @Override
    public String contentType() {
        return eventDecoderFactory.contentType();
    }
}
