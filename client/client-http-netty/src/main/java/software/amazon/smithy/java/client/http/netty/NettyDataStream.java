/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * A DataStream implementation that will buffer the ByteBuffer until a subscription requests items.
 */
final class NettyDataStream implements DataStream {
    private static final NettyLogger LOGGER = NettyLogger.getLogger(NettyDataStream.class);

    private final String contentType;
    private final long contentLength;
    private final Channel channel;

    private final ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<>();
    private volatile Flow.Subscriber<? super ByteBuffer> subscriber;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private final AtomicLong requested = new AtomicLong(0);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean errored = new AtomicBoolean(false);
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    NettyDataStream(String contentType, long contentLength, Channel channel) {
        this.contentType = contentType;
        this.contentLength = contentLength;
        this.channel = channel;
    }

    @Override
    public boolean isReplayable() {
        // Streaming data cannot be replayed
        return false;
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    public void enqueue(ByteBuffer item) {
        queue.offer(item);
        tryDeliver();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (!subscribed.compareAndSet(false, true)) {
            subscriber.onError(new IllegalStateException("Already subscribed"));
            return;
        }

        this.subscriber = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException("Request must be positive"));
                    errored.set(true);
                    return;
                }
                requested.addAndGet(n);
                tryDeliver();
            }

            @Override
            public void cancel() {
                subscribed.set(false);
            }
        });
    }

    private void tryDeliver() {
        if (subscriber == null) {
            return;
        }
        if (completed.get()) {
            return;
        }
        if (errored.get()) {
            Throwable pendingError = error.get();
            if (pendingError != null && error.compareAndSet(pendingError, null)) {
                subscriber.onError(pendingError);
            }
            return;
        }

        while (requested.get() > 0 && !queue.isEmpty()) {
            var item = queue.poll();
            if (item != null) {
                requested.decrementAndGet();
                subscriber.onNext(item);
            }
        }
    }

    public void complete() {
        completed.set(true);
        if (subscriber != null) {
            subscriber.onComplete();
        }
    }

    /**
     * Called by NettyHttpResponseHandler when HttpContent is received.
     */
    void onContent(HttpContent content) {
        if (completed.get() || errored.get()) {
            return;
        }

        try {
            var byteBuf = content.content();
            if (byteBuf.isReadable()) {
                // Convert Netty's ByteBuf to Java's ByteBuffer
                var buffer = ByteBuffer.allocate(byteBuf.readableBytes());
                byteBuf.getBytes(byteBuf.readerIndex(), buffer);
                buffer.flip();
                LOGGER.debug(channel, "onNext from netty subscriber");
                enqueue(buffer);
            }

            // Complete stream on last content
            if (content instanceof LastHttpContent) {
                complete();
                closeChannel();
                LOGGER.info(channel, "Stream completed");
            }
        } catch (Exception e) {
            LOGGER.error(channel, "Error processing content", e);
            onError(ClientTransport.remapExceptions(e));
            errored.set(true);
        }
    }

    /**
     * Called by NettyHttpResponseHandler when an error occurs.
     */
    void onError(Throwable throwable) {
        if (completed.get() || !errored.compareAndSet(false, true)) {
            return;
        }
        error.compareAndSet(null, throwable);
        closeChannel();
    }

    void closeChannel() {
        var pool = channel.attr(NettyConstants.CHANNEL_POOL).get();
        if (pool != null) {
            pool.release(channel);
        } else {
            LOGGER.warn(channel,
                    "The channel is not associated a channel pool. " +
                            "It will be closed but cannot be released.");
            channel.close();
        }
    }
}
