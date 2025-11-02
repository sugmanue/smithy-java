/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty.h2;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.handler.codec.http2.Http2GoAwayFrame;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.concurrent.Promise;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import software.amazon.smithy.java.client.core.error.TransportException;
import software.amazon.smithy.java.client.http.netty.NettyLogger;

/**
 * An HTTP/2 channel state used to create new streams, and, keep track of each of the streams, and handling the
 * closing of streams and the parent.
 */
class MultiplexedChannelState {
    private static final NettyLogger LOGGER = NettyLogger.getLogger(MultiplexedChannelState.class);
    private final Channel parentChannel;
    private final int maxConcurrentStreams;
    private final Map<ChannelId, Http2StreamChannel> children = new HashMap<>();
    private final AtomicInteger acquiredStreams = new AtomicInteger();
    private volatile State state = State.OPEN;
    private volatile int lastStreamId;

    MultiplexedChannelState(Channel parentChannel, int maxConcurrentStreams) {
        this.parentChannel = parentChannel;
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public Channel getConnection() {
        return parentChannel;
    }

    /**
     * Returns true if a new stream could be acquired successfully. This given {@link Promise}
     * will be notified once the acquire is successful and failed otherwise.
     * This method will return false when there are no more streams available or when
     * the parent channel is not-longer open to create new streams.
     *
     * @param promise The promise for the acquired channel
     * @return true if the acquisition is successful, false otherwise.
     */
    public boolean tryAcquire(Promise<Channel> promise) {
        if (!tryAcquireClaim()) {
            return false;
        }
        acquireClaimed(promise);
        return true;
    }

    /**
     * Handle a {@link Http2GoAwayFrame} on this connection, preventing new streams from being created on it, and closing any
     * streams newer than the last-stream-id on the go-away frame.
     */
    public void handleGoAway(int lastStreamId, GoAwayException exception) {
        parentChannel.eventLoop().execute(() -> {
            this.lastStreamId = lastStreamId;
            if (state == State.CLOSED) {
                return;
            }
            if (state == State.OPEN) {
                state = State.CLOSED_TO_NEW;
            }
            var childrenToClose = new ArrayList<>(children.values());
            for (var child : childrenToClose) {
                if (child.stream().id() > lastStreamId) {
                    child.pipeline().fireUserEventTriggered(exception);
                }
            }
        });
    }

    /**
     * Closes the channel to new streams.
     */
    public void closeToNewStreams() {
        if (state == State.OPEN) {
            state = State.CLOSED_TO_NEW;
        }
    }

    /**
     * Returns true if the channel is not open and there're no more active streams on it.
     *
     * @return true if the channel can be closed and released to the connection pool.
     */
    public boolean canBeClosedAndReleased() {
        return state != State.OPEN && acquiredStreams.get() == 0;
    }

    /**
     * Close all registered streams, and disallow new streams from being created. If an exception is passed
     * it gets delivered to all streams.
     */
    public void closeStreams(Throwable t) {
        if (t != null) {
            closeAndExecuteOnChildChannels(ch -> ch.pipeline().fireExceptionCaught(decorateConnectionException(t)));
        } else {
            closeAndExecuteOnChildChannels(ChannelOutboundInvoker::close);
        }
    }

    private boolean tryAcquireClaim() {
        if (state != State.OPEN) {
            LOGGER.info(parentChannel, "Multiplexed channel is not open for new streams");
            return false;
        }
        for (var attempt = 0; attempt < 5; ++attempt) {
            var streams = acquiredStreams.get();
            if (streams >= maxConcurrentStreams) {
                return false;
            }
            if (!acquiredStreams.compareAndSet(streams, streams + 1)) {
                // try again
                continue;
            }
            return true;
        }
        LOGGER.warn(parentChannel, "Failed to claim a new stream for multiplexed channel");
        return false;
    }

    private void acquireClaimed(Promise<Channel> promise) {
        promise.addListener(future -> {
            if (!future.isSuccess()) {
                releaseClaim();
            }
        });
        parentChannel.eventLoop().execute(() -> {
            if (state != State.OPEN) {
                String message;
                if (state == State.CLOSED_TO_NEW) {
                    message =
                            String.format(
                                    "Connection %s received GOAWAY with lastStreamId %d. Unable to open new "
                                            + "streams on this connection.",
                                    parentChannel,
                                    lastStreamId);
                } else {
                    message = String.format("Connection %s was closed while acquiring new stream.", parentChannel);
                }
                LOGGER.warn(parentChannel, message);
                promise.setFailure(new TransportException(message));
                return;
            }
            var future = new Http2StreamChannelBootstrap(parentChannel).open();
            future.addListener(f -> {
                if (!f.isSuccess()) {
                    promise.setFailure(f.cause());
                    return;
                }
                var stream = (Http2StreamChannel) f.getNow();
                // Configure stream pipeline
                var pipeline = stream.pipeline();
                pipeline.addLast(new Http2ToHttpInboundAdapter());
                pipeline.addLast(new HttpToHttp2OutboundAdapter());
                pipeline.addLast(Http2StreamExceptionHandler.getInstance());
                children.put(stream.id(), stream);
                promise.setSuccess(stream);
            });
        });
    }

    private void closeAndExecuteOnChildChannels(Consumer<Channel> streamConsumer) {
        parentChannel.eventLoop().execute(() -> {
            if (state == State.CLOSED) {
                return;
            }
            state = State.CLOSED;
            // Create a copy of the children, because they may be modified by the consumer.
            var streamsToClose = new ArrayList<>(children.values());
            for (var streamChannel : streamsToClose) {
                streamConsumer.accept(streamChannel);
            }
        });
    }

    public void closeAndReleaseStream(Channel childChannel) {
        childChannel.close();
        parentChannel.eventLoop().execute(() -> {
            children.remove(childChannel.id());
            releaseClaim();
        });
    }

    private Throwable decorateConnectionException(Throwable t) {
        var message =
                String.format("An error occurred on the connection: %s, [channel: %s]. All streams will be closed",
                        t,
                        parentChannel.id());
        return new TransportException(message, t);
    }

    private void releaseClaim() {
        if (acquiredStreams.decrementAndGet() < 0) {
            assert false;
            // TODO, log here
            acquiredStreams.incrementAndGet();
        }
    }

    enum State {
        /**
         * The channel is open.
         */
        OPEN,
        /**
         * The channel is closed for new streams. This will be set whenever an I/O exception
         * is caught in the channel.
         */
        CLOSED_TO_NEW,
        /**
         * The channel is closed.
         */
        CLOSED
    }
}
