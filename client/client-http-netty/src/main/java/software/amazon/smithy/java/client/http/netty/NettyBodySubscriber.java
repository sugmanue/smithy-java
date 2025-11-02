/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.http.api.HttpResponse;

/**
 * Flow.Subscriber that streams request body data to a Netty channel using chunked transfer encoding.
 */
final class NettyBodySubscriber implements Flow.Subscriber<ByteBuffer> {
    private static final NettyLogger LOGGER = NettyLogger.getLogger(NettyBodySubscriber.class);
    private final Channel channel;
    private final CompletableFuture<HttpResponse> responseFuture;
    private Flow.Subscription subscription;

    NettyBodySubscriber(Channel channel, CompletableFuture<HttpResponse> responseFuture) {
        this.channel = channel;
        this.responseFuture = responseFuture;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        LOGGER.debug(channel, "onSubscribe, requesting from subscription");
        subscription.request(1);
    }

    @Override
    public void onNext(ByteBuffer item) {
        try {
            LOGGER.debug(channel, "onNext, sending item");
            if (item.hasRemaining()) {
                // Convert ByteBuffer to Netty ByteBuf and send as HTTP chunk
                var content = new DefaultHttpContent(Unpooled.wrappedBuffer(item));
                channel.writeAndFlush(content).addListener(future -> {
                    if (future.isSuccess()) {
                        subscription.request(1);
                        return;
                    }
                    var cause = future.cause();
                    LOGGER.error(channel, "Failed to write chunk", cause);
                    responseFuture.completeExceptionally(ClientTransport.remapExceptions(cause));
                    subscription.cancel();
                });
            } else {
                subscription.request(1);
            }
        } catch (Exception e) {
            LOGGER.error(channel, "Error processing chunk", e);
            responseFuture.completeExceptionally(ClientTransport.remapExceptions(e));
            subscription.cancel();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error(channel, "Error in streaming body", throwable);
        responseFuture.completeExceptionally(ClientTransport.remapExceptions(throwable));
        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    @Override
    public void onComplete() {
        // Send the final chunk to indicate end of stream
        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(future -> {
            if (future.isSuccess()) {
                return;
            }
            var cause = future.cause();
            LOGGER.warn(channel, "Failed to write final chunk", cause);
            responseFuture.completeExceptionally(ClientTransport.remapExceptions(cause));
        });
    }
}
