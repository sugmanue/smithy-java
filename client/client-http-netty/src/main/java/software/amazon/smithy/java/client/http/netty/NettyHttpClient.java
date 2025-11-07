/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import static software.amazon.smithy.java.client.http.netty.NettyConstants.HTTP_VERSION_FUTURE;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import software.amazon.smithy.java.client.core.ClientTransport;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.io.ByteBufferUtils;

/**
 * The HTTP client responsible from sending requests, and, handling the response.
 */
final class NettyHttpClient implements Closeable {

    private static final NettyLogger LOGGER = NettyLogger.getLogger(NettyHttpClient.class);
    private final NettyHttpClientTransport.Configuration config;
    private final ChannelPoolMap channelPoolMap;

    NettyHttpClient(NettyHttpClientTransport.Builder builder) {
        this.config = builder.buildConfiguration();
        this.channelPoolMap = new ChannelPoolMap(config);
    }

    private static void sendHttpRequest(
            Channel channel,
            HttpRequest request,
            NettyHttpClientTransport.Configuration config,
            CompletableFuture<HttpResponse> responseFuture
    ) {
        var pipeline = channel.pipeline();
        pipeline.addFirst(new WriteTimeoutHandler(config.writeTimeout().toMillis(), TimeUnit.MILLISECONDS));
        if (request.body().hasKnownLength()) {
            // Known length - send as FullHttpRequest
            sendFullHttpRequest(channel, request, responseFuture);
        } else {
            // Unknown length - send as streaming request
            sendStreamedHttpRequest(channel, request, responseFuture);
        }
        LOGGER.trace(channel, "HTTP request sent scheduled for {}", request.uri());
    }

    private static void sendFullHttpRequest(
            Channel channel,
            HttpRequest request,
            CompletableFuture<HttpResponse> responseFuture
    ) {
        var httpVersion = getHttpVersion(channel);
        LOGGER.trace(channel, "Sending a full HTTP request using version {}", httpVersion);
        var method = HttpMethod.valueOf(request.method());
        var path = getPath(request.uri());
        var content = Unpooled.EMPTY_BUFFER;
        if (request.body().contentLength() > 0) {
            var bodyBytes = ByteBufferUtils.getBytes(request.body().waitForByteBuffer());
            content = Unpooled.wrappedBuffer(bodyBytes);
        }
        // All requests start out as HTTP/1.1 objects, even if they will
        // ultimately be sent over HTTP2. Conversion to H2 is handled at a
        // later stage if necessary; see HttpToHttp2OutboundAdapter.
        var nettyRequest = new DefaultFullHttpRequest(
                io.netty.handler.codec.http.HttpVersion.HTTP_1_1,
                method,
                path,
                content);
        setHeaders(nettyRequest, request);
        if (content.readableBytes() > 0) {
            nettyRequest.headers().set("Content-Length", content.readableBytes());
        }
        channel.writeAndFlush(nettyRequest).addListener(writeFuture -> {
            if (!writeFuture.isSuccess()) {
                var cause = writeFuture.cause();
                LOGGER.warn(channel, "Request write failed", cause);
                responseFuture.completeExceptionally(
                        ClientTransport.remapExceptions(cause));
            } else {
                LOGGER.trace(channel, "Write succeeded");
            }
        });
    }

    private static void sendStreamedHttpRequest(
            Channel channel,
            HttpRequest request,
            CompletableFuture<HttpResponse> responseFuture
    ) {
        var httpVersion = getHttpVersion(channel);
        LOGGER.trace(channel, "Sending streaming HTTP request using version {}", httpVersion);
        var method = HttpMethod.valueOf(request.method());
        var path = getPath(request.uri());
        // All requests start out as HTTP/1.1 objects, even if they will
        // ultimately be sent over HTTP2. Conversion to H2 is handled at a
        // later stage if necessary; see HttpToHttp2OutboundAdapter.
        var nettyRequest = new DefaultHttpRequest(io.netty.handler.codec.http.HttpVersion.HTTP_1_1, method, path);
        setHeaders(nettyRequest, request);
        if (httpVersion == HttpVersion.HTTP_2) {
            // Set scheme for HTTP/2 conversion
            nettyRequest.headers()
                    .set(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(),
                            request.uri().getScheme());
        }

        // Send the initial request to create the stream (without closing it)
        channel.writeAndFlush(nettyRequest).addListener(writeFuture -> {
            if (writeFuture.isSuccess()) {
                // Now stream the body data
                LOGGER.trace(channel, "Write succeeded");
                request.body().subscribe(new NettyBodySubscriber(channel, responseFuture));
            } else {
                var cause = writeFuture.cause();
                LOGGER.warn(channel, "Request write failed", cause);
                responseFuture.completeExceptionally(
                        ClientTransport.remapExceptions(cause));
            }
        });
    }

    private static void setHeaders(io.netty.handler.codec.http.HttpRequest nettyRequest, HttpRequest request) {
        // Set headers
        var nettyHeaders = nettyRequest.headers();
        for (var entry : request.headers().map().entrySet()) {
            for (var value : entry.getValue()) {
                nettyHeaders.add(entry.getKey(), value);
            }
        }

        // Set Host header if not already present
        if (!nettyHeaders.contains("Host")) {
            String host = request.uri().getHost();
            int port = request.uri().getPort();
            if (port != -1 && port != 80 && port != 443) {
                host += ":" + port;
            }
            nettyHeaders.set("Host", host);
        }
    }

    private static HttpVersion getHttpVersion(Channel ch) {
        var channel = getParentIfPresent(ch);
        return channel.attr(HTTP_VERSION_FUTURE).get().join();
    }

    private static Channel getParentIfPresent(Channel ch) {
        var parent = ch.parent();
        if (parent == null) {
            return ch;
        }
        return parent;
    }

    private static String getPath(URI uri) {
        var path = uri.getRawPath();
        if (path == null) {
            path = "/";
        }
        var query = uri.getRawQuery();
        if (query != null) {
            path += "?" + query;
        }
        return path;
    }

    /**
     * Sends the HTTP request and returns a future with the HTTP response.
     */
    public CompletableFuture<HttpResponse> send(HttpRequest request) {
        var responseFuture = new CompletableFuture<HttpResponse>();
        var channelFuture = channelPoolMap.acquire(request.uri());
        var handler = new ChannelAcquiredHandler(request, responseFuture, config);
        channelFuture.addListener(handler::handleChannelAcquired);
        return responseFuture;
    }

    @Override
    public void close() {
        channelPoolMap.close();
    }

    static class ChannelAcquiredHandler {
        private final HttpRequest request;
        private final CompletableFuture<HttpResponse> responseFuture;
        private final NettyHttpClientTransport.Configuration config;

        ChannelAcquiredHandler(
                HttpRequest request,
                CompletableFuture<HttpResponse> responseFuture,
                NettyHttpClientTransport.Configuration config
        ) {
            this.request = request;
            this.responseFuture = responseFuture;
            this.config = config;
        }

        void handleChannelAcquired(Future<? super Channel> future) {
            if (!future.isSuccess()) {
                var cause = future.cause();
                LOGGER.warn(null, "Failed to acquire a channel for URI " + request.uri(), cause);
                responseFuture.completeExceptionally(ClientTransport.remapExceptions(cause));
                return;
            }
            var channel = (Channel) future.getNow();
            LOGGER.trace(channel, "Successfully acquired channel for URI {}", request.uri());
            channel.pipeline().addFirst(new ReadTimeoutHandler(config.readTimeout().toMillis(), TimeUnit.MILLISECONDS));
            channel.pipeline().addLast(new NettyHttpResponseHandler(responseFuture));
            sendHttpRequest(channel, request, config, responseFuture);
        }
    }
}
