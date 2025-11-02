/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import static software.amazon.smithy.java.client.http.netty.NettyConstants.CHANNEL_POOL;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.HTTP11_CODEC;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.HTTP_VERSION_FUTURE;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.PROTOCOL_NEGOTIATION;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.SSL;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.SSL_CLOSE_COMPLETE;
import static software.amazon.smithy.java.client.http.netty.NettyConstants.SSL_HANDSHAKE;

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.smithy.java.http.api.HttpVersion;

/**
 * Initializes the channel pipeline adding the handlers needed according with the
 * configuration and the URI.
 */
final class ChannelPipelineInitializer extends AbstractChannelPoolHandler {
    private final NettyHttpClientTransport.Configuration config;
    private final SslContext sslContext;
    private final URI uri;
    private final AtomicReference<ChannelPool> channelPoolRef;

    ChannelPipelineInitializer(
            NettyHttpClientTransport.Configuration config,
            URI uri,
            SslContext sslContext,
            AtomicReference<ChannelPool> channelPoolRef
    ) {
        this.config = config;
        this.uri = uri;
        this.sslContext = sslContext;
        this.channelPoolRef = channelPoolRef;
    }

    @Override
    public void channelCreated(Channel ch) {
        ch.attr(HTTP_VERSION_FUTURE).set(new CompletableFuture<>());
        var host = uri.getHost();
        var isHttps = uri.getScheme().equalsIgnoreCase("https");
        var port = uri.getPort() == -1 ? (isHttps ? 443 : 80) : uri.getPort();
        var pipeline = ch.pipeline();
        // TODO (sugmanue, 2025/11/03): Add support for HTTP/2 without ALPN
        //  and without HTTPS (aka h2c)
        if (isHttps) {
            pipeline.addLast(SSL, sslContext.newHandler(ch.alloc(), host, port));
            pipeline.addLast(SSL_HANDSHAKE, new NettySslHandshakeHandler(config.httpVersion()));
            pipeline.addLast(SSL_CLOSE_COMPLETE, new NettySslCloseCompletionHandler());
            if (config.httpVersion() == HttpVersion.HTTP_2) {
                pipeline.addLast(PROTOCOL_NEGOTIATION, new NettyProtocolNegotiationHandler(config, channelPoolRef));
            } else {
                ch.attr(CHANNEL_POOL).set(channelPoolRef.get());
            }
        } else {
            pipeline.addLast(HTTP11_CODEC, new HttpClientCodec());
            ch.attr(HTTP_VERSION_FUTURE).get().complete(HttpVersion.HTTP_1_1);
            ch.attr(CHANNEL_POOL).set(channelPoolRef.get());
        }
        pipeline.addLast(new LoggingHandler(LogLevel.TRACE));
    }
}
