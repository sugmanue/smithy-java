/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty.h2;

import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.CHANNEL_POOL;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.HTTP2_CONNECTION;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.HTTP2_INITIAL_WINDOW_SIZE;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.HTTP2_MULTIPLEXED_CONNECTION_POOL;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.HTTP_VERSION_FUTURE;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.MAX_CONCURRENT_STREAMS;
import static software.amazon.smithy.java.client.http.netty.h2.NettyHttp2Constants.MULTIPLEXED_CHANNEL;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.smithy.java.client.core.error.TransportException;
import software.amazon.smithy.java.client.core.error.TransportProtocolException;
import software.amazon.smithy.java.client.http.netty.NettyLogger;
import software.amazon.smithy.java.http.api.HttpVersion;

/**
 * A specialized connection pool for multiplexed HTTP/2 connections.
 */
class Http2MultiplexedConnectionPool implements ChannelPool {
    private static final AttributeKey<Boolean> RELEASED = AttributeKey.valueOf(
            "software.amazon.awssdk.http.nio.netty.internal.http2.Http2MultiplexedChannelPool.RELEASED");
    private static final NettyLogger LOGGER = NettyLogger.getLogger(Http2MultiplexedConnectionPool.class);

    private final ChannelPool channelPool;
    private final EventLoopGroup eventLoopGroup;
    private final Set<MultiplexedChannelState> channels;
    private final AtomicBoolean closed = new AtomicBoolean();

    Http2MultiplexedConnectionPool(ChannelPool channelPool, EventLoopGroup eventLoopGroup) {
        this.channelPool = channelPool;
        this.eventLoopGroup = eventLoopGroup;
        this.channels = ConcurrentHashMap.newKeySet();
    }

    @Override
    public Future<Channel> acquire() {
        Promise<Channel> promise = eventLoopGroup.next().newPromise();
        return acquire(promise);
    }

    @Override
    public Future<Channel> acquire(Promise<Channel> promise) {
        if (closed.get()) {
            promise.setFailure(new TransportException("Channel pool is closed"));
        }
        for (var multiplexedChannel : channels) {
            if (multiplexedChannel.tryAcquire(promise)) {
                return promise;
            }
        }
        // We need to create a new parent connection
        acquireStreamOnNewConnection(promise);
        return promise;
    }

    @Override
    public Future<Void> release(Channel channel) {
        Promise<Void> promise = eventLoopGroup.next().newPromise();
        return release(channel, promise);
    }

    @Override
    public Future<Void> release(Channel stream, Promise<Void> promise) {
        var parent = stream.parent();
        if (parent == null) {
            // This isn't a h2 stream channel. Warn about it and close the channel.
            LOGGER.warn(stream, "Release called on a non-h2 stream, closing the channel");
            closeAndReleaseParent(stream);
            return promise
                    .setFailure(new TransportException("Channel (" + stream + ") is not a h2 stream."));
        }

        var multiplexedChannel = parent.attr(MULTIPLEXED_CHANNEL).get();
        if (multiplexedChannel == null) {
            // This is a h2 stream channel, but there is no attached multiplexed channel,
            // which there should be if it was from this pool. Close it and log an error.
            var message = "Channel (" + stream + ") is not associated with a multiplexed channel. "
                    + "It will be closed, but cannot be released within this pool.";
            var exception = new TransportException(message);
            LOGGER.warn(stream, message);
            stream.close();
            return promise.setFailure(exception);
        }
        multiplexedChannel.closeAndReleaseStream(stream);
        if (multiplexedChannel.canBeClosedAndReleased()) {
            // There are no more outstanding streams in this channel,
            // and it's ready to be closed.
            return closeAndReleaseParent(parent, null, promise);
        }
        return promise.setSuccess(null);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            var closeCompleteFuture = submitClose();
            try {
                if (!closeCompleteFuture.await(5, TimeUnit.SECONDS)) {
                    throw new TransportException("Event loop didn't close after 5 seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new TransportException(e);
            }

            var exception = closeCompleteFuture.cause();
            if (exception != null) {
                throw new TransportException("Failed to close channel pool.", exception);
            }
        }
    }

    private Future<?> submitClose() {
        var closeEventLoop = eventLoopGroup.next();
        var closeFinishedPromise = closeEventLoop.newPromise();
        closeEventLoop.execute(() -> {
            var promiseCombiner = new PromiseCombiner(closeEventLoop);
            var channelsToRemove = new ArrayList<>(channels);
            for (var multiplexedChannel : channelsToRemove) {
                promiseCombiner.add(closeAndReleaseParent(multiplexedChannel.getConnection()));
            }
            var releaseAllChannelsPromise = closeEventLoop.<Void>newPromise();
            promiseCombiner.finish(releaseAllChannelsPromise);
            releaseAllChannelsPromise.addListener(f -> {
                channelPool.close();
                closeFinishedPromise.setSuccess(null);
            });
        });

        return closeFinishedPromise;
    }

    private void acquireStreamOnNewConnection(Promise<Channel> promise) {
        var newConnectionAcquire = channelPool.acquire();
        newConnectionAcquire.addListener(f -> {
            if (!newConnectionAcquire.isSuccess()) {
                promise.setFailure(newConnectionAcquire.cause());
                return;
            }

            var parentChannel = newConnectionAcquire.getNow();
            // When the protocol future is completed on the new connection,
            // we're ready for new streams to be added to it.
            var protocol = parentChannel.attr(HTTP_VERSION_FUTURE).get().get();

            if (protocol == HttpVersion.HTTP_2) {
                parentChannel.attr(HTTP2_MULTIPLEXED_CONNECTION_POOL).set(this);
                acquireStreamOnFreshConnection(promise, parentChannel, protocol);
            } else {
                var exception = new TransportProtocolException("Unsupported protocol: " + protocol);
                failAndCloseParent(promise, parentChannel, exception);
            }
        });
    }

    private void acquireStreamOnFreshConnection(Promise<Channel> promise, Channel parentChannel, HttpVersion protocol) {
        try {
            var maxStreams = parentChannel.attr(MAX_CONCURRENT_STREAMS).get();
            var multiplexedChannel = new MultiplexedChannelState(parentChannel, maxStreams.intValue());
            parentChannel.attr(MULTIPLEXED_CHANNEL).set(multiplexedChannel);

            var streamPromise = parentChannel.eventLoop().<Channel>newPromise();

            if (!acquireStreamOnInitializedConnection(multiplexedChannel, streamPromise)) {
                failAndCloseParent(promise,
                        parentChannel,
                        new TransportException("Connection was closed while creating a new stream."));
                return;
            }

            streamPromise.addListener(f -> {
                if (!streamPromise.isSuccess()) {
                    promise.setFailure(streamPromise.cause());
                    return;
                }

                var stream = streamPromise.getNow();
                // Before we cache the connection, make sure that exceptions on the connection will remove it from the cache.
                parentChannel.pipeline().addLast(ReleaseOnExceptionHandler.getInstance());
                cacheConnection(stream, multiplexedChannel, promise);
            });
        } catch (Throwable e) {
            failAndCloseParent(promise, parentChannel, e);
        }
    }

    private boolean acquireStreamOnInitializedConnection(
            MultiplexedChannelState multiplexedChannel,
            Promise<Channel> promise
    ) {
        var acquirePromise = multiplexedChannel.getConnection().eventLoop().<Channel>newPromise();
        if (!multiplexedChannel.tryAcquire(acquirePromise)) {
            return false;
        }
        acquirePromise.addListener(future -> {
            if (!future.isSuccess()) {
                promise.setFailure(future.cause());
                return;
            }
            Channel stream = (Channel) future.getNow();
            stream.attr(HTTP2_MULTIPLEXED_CONNECTION_POOL).set(this);
            stream.attr(CHANNEL_POOL).set(this);
            stream.attr(MULTIPLEXED_CHANNEL).set(multiplexedChannel);
            tryExpandConnectionWindow(stream.parent());
            promise.setSuccess(stream);
        });
        return true;
    }

    private void failAndCloseParent(Promise<Channel> promise, Channel parentChannel, Throwable throwable) {
        LOGGER.warn(parentChannel, "Channel acquiring failed, closing connection", throwable);
        promise.setFailure(throwable);
        closeAndReleaseParent(parentChannel);
    }

    private void cacheConnection(
            Channel stream,
            MultiplexedChannelState multiplexedChannel,
            Promise<Channel> promise
    ) {
        var parentChannel = stream.parent();

        channels.add(multiplexedChannel);

        if (closed.get()) {
            // We were closed while we were setting up. Make sure everything here is cleaned up properly.
            var exception = new TransportException("Connection pool was closed while creating a new stream.");
            failAndCloseParent(promise,
                    parentChannel,
                    exception);
            return;
        }
        promise.setSuccess(stream);
    }

    private Future<Void> closeAndReleaseParent(Channel parentChannel) {
        return closeAndReleaseParent(parentChannel, null, parentChannel.eventLoop().newPromise());
    }

    public Future<Void> closeAndReleaseParent(Channel parentChannel, Throwable cause) {
        return closeAndReleaseParent(parentChannel, cause, parentChannel.eventLoop().newPromise());
    }

    private Future<Void> closeAndReleaseParent(Channel parentChannel, Throwable cause, Promise<Void> resultPromise) {
        if (parentChannel.parent() != null) {
            // This isn't a parent channel. Notify that something is wrong.
            var message = "Channel (" + parentChannel + ") is not a parent channel. It will be closed, "
                    + "but cannot be released within this pool.";
            var exception = new TransportException(message, cause);
            LOGGER.warn(parentChannel, message, cause);
            parentChannel.close();
            return resultPromise.setFailure(exception);
        }

        var multiplexedChannel = parentChannel.attr(MULTIPLEXED_CHANNEL).get();

        // We may not have a multiplexed channel if the parent channel hasn't been fully initialized.
        if (multiplexedChannel != null) {
            multiplexedChannel.closeStreams(cause);
            channels.remove(multiplexedChannel);
        }
        parentChannel.close();
        if (parentChannel.attr(RELEASED).getAndSet(Boolean.TRUE) == null) {
            return channelPool.release(parentChannel, resultPromise);
        }
        return resultPromise.setSuccess(null);
    }

    /**
     * This can be called to request the expansion of the window size published by this endpoint.
     */
    private void tryExpandConnectionWindow(Channel parentChannel) {
        var task = new AbstractChannelHoldingTask(parentChannel) {
            @Override
            public void run() {
                var http2Connection = this.channel.attr(HTTP2_CONNECTION).get();
                var initialWindowSize = this.channel.attr(HTTP2_INITIAL_WINDOW_SIZE).get();
                var connectionStream = http2Connection.connectionStream();
                LOGGER.debug(this.channel, "Expanding connection window of size {}", initialWindowSize);
                try {
                    var localFlowController = http2Connection.local().flowController();
                    localFlowController.incrementWindowSize(connectionStream, initialWindowSize);
                } catch (Http2Exception e) {
                    LOGGER.warn(this.channel, "Failed to expand window of size {}", initialWindowSize, e);
                }
            }
        };
        parentChannel.eventLoop().execute(task);
    }
}
