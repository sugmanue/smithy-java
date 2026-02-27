/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

/**
 * Factory to create decoders to decode bytes into frames and frames into messages.
 *
 * @param <F> The type of the frame.
 */
public interface EventDecoderFactory<F extends Frame<?>> {

    /**
     * Returns a new event decoder.
     *
     * @return a new event decoder.
     */
    EventDecoder<F> newEventDecoder();

    /**
     * Returns a new frame decoder.
     *
     * @return a new frame decoder.
     */
    FrameDecoder<F> newFrameDecoder();
}
