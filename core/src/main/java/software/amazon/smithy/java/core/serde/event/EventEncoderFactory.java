/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

/**
 * Factory to create encoders to encode the events into frames and frames into bytes.
 *
 * @param <F> The type of the frame.
 */
public interface EventEncoderFactory<F extends Frame<?>> {

    /**
     * Returns a new event encoder.
     *
     * @return a new event encoder
     */
    EventEncoder<F> newEventEncoder();

    /**
     * Returns a new frame encoder.
     *
     * @return a new frame encoder
     */
    FrameEncoder<F> newFrameEncoder();

    /**
     * Get the content type for this frame type.
     *
     * @return a content-type.
     */
    String contentType();

    /**
     * Composes the factory with the given frame processor. The processor be composed with other existing processors
     * in the order those are created. For instance
     *
     * {@snippet java:
     *    // This factory will first apply the GzipFrameProcessor and then the SigningFrameProcessor.
     *    var factory = awsEncoderFactory.withFrameProcessor(new GzipFrameProcessor())
     *                            .withFrameProcessor(new SigningFrameProcessor());
     *
     * }
     *
     * @param frameProcessor the frame processor
     * @return the composed factory
     */
    default EventEncoderFactory<F> withFrameProcessor(FrameProcessor<F> frameProcessor) {
        return new ProcessingEventEncoderFactory<>(this, frameProcessor);
    }
}
