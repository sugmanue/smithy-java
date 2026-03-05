/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import software.amazon.smithy.java.core.schema.SerializableStruct;

/**
 * Composes a event encoder with a frame processor.
 *
 * @param <F> the type of the frame
 */
final class ProcessingEventEncoder<F extends Frame<?>> implements EventEncoder<F> {
    private final EventEncoder<F> eventEncoder;
    private final FrameProcessor<F> frameProcessor;

    ProcessingEventEncoder(EventEncoder<F> eventEncoder, FrameProcessor<F> frameProcessor) {
        this.eventEncoder = eventEncoder;
        this.frameProcessor = frameProcessor;
    }

    @Override
    public F encode(SerializableStruct item) {
        F frame = eventEncoder.encode(item);
        return frameProcessor.transformFrame(frame);
    }

    @Override
    public F encodeFailure(Throwable exception) {
        F frame = eventEncoder.encodeFailure(exception);
        return frameProcessor.transformFrame(frame);
    }

    @Override
    public F closingFrame() {
        F closingFrame = eventEncoder.closingFrame();
        if (closingFrame != null) {
            return closingFrame;
        }
        return frameProcessor.closingFrame();
    }
}
