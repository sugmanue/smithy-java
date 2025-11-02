/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpVersion;

/**
 * Netty channel handler that processes HTTP responses. Streams responses only when
 * transfer-encoding is chunked or content-length is unknown, otherwise buffers the complete response.
 */
final class NettyHttpResponseHandler extends SimpleChannelInboundHandler<Object> {

    private static final NettyLogger LOGGER = NettyLogger.getLogger(NettyHttpResponseHandler.class);

    private final CompletableFuture<software.amazon.smithy.java.http.api.HttpResponse> responseFuture;
    private NettyDataStream streamingDataStream;
    private int statusCode;
    private HttpVersion httpVersion;

    NettyHttpResponseHandler(CompletableFuture<software.amazon.smithy.java.http.api.HttpResponse> responseFuture) {
        this.responseFuture = responseFuture;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof HttpResponse response) {
                statusCode = response.status().code();
                httpVersion = convertHttpVersion(response.protocolVersion());
                var contentType = getContentType(response);
                var contentLength = getContentLength(response);
                var smithyHeaders = toSmithyHeaders(response);
                streamingDataStream = new NettyDataStream(contentType, contentLength, ctx.channel());
                var smithyResponse =
                        software.amazon.smithy.java.http.api.HttpResponse.builder()
                                .httpVersion(httpVersion)
                                .statusCode(statusCode)
                                .headers(smithyHeaders)
                                .body(streamingDataStream)
                                .build();

                responseFuture.complete(smithyResponse);
                LOGGER.debug(ctx.channel(), "Completed response future with streaming body: status: {}", statusCode);
            } else if (msg instanceof HttpContent content) {
                LOGGER.debug(ctx.channel(), "Content received");
                streamingDataStream.onContent(content);
            }
        } catch (Exception e) {
            LOGGER.error(ctx.channel(), "Error processing response", e);
            if (streamingDataStream != null) {
                streamingDataStream.onError(e);
            }
            responseFuture.completeExceptionally(e);
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(ctx.channel(), "Exception in response handler", cause);
        if (streamingDataStream != null) {
            streamingDataStream.onError(cause);
        }
        responseFuture.completeExceptionally(cause);
        ctx.close();
    }

    private HttpVersion convertHttpVersion(io.netty.handler.codec.http.HttpVersion nettyVersion) {
        if (nettyVersion.equals(io.netty.handler.codec.http.HttpVersion.HTTP_1_0)) {
            // Map 1.0 to 1.1, Smithy doesn't support 1.0
            return HttpVersion.HTTP_1_1;
        } else if (nettyVersion.equals(io.netty.handler.codec.http.HttpVersion.HTTP_1_1)) {
            return HttpVersion.HTTP_1_1;
        } else {
            return HttpVersion.HTTP_2; // Default to HTTP/2 for other versions
        }
    }

    private HttpHeaders toSmithyHeaders(HttpResponse response) {
        var smithyHeaders = HttpHeaders.ofModifiable();
        var nettyHeaders = response.headers();
        for (var entry : nettyHeaders) {
            smithyHeaders.addHeader(entry.getKey(), entry.getValue());
        }
        return smithyHeaders;
    }

    private String getContentType(HttpResponse response) {
        var contentType = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
        return contentType;
    }

    private long getContentLength(HttpResponse response) {
        var contentLength = response.headers().get(HttpHeaderNames.CONTENT_LENGTH.toString());
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                LOGGER.warn(null, "Failed to parse content length header '{}'", contentLength);
            }
        }
        return 0;
    }
}
