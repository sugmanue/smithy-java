/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty.h2;

import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.util.AttributeKey;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.http.api.HttpVersion;

/**
 * Constant values used in the Netty HTTP/2 client.
 */
final class NettyHttp2Constants {

    // Handler names
    public static final String HTTP2_FRAME_CODEC = "smithy.netty.http2-frame-codec";
    public static final String HTTP2_MULTIPLEX = "smithy.netty.http2-multiplexer";
    public static final String HTTP2_SETTINGS = "smithy.netty.http2-settings";
    public static final String HTTP2_ACK_SETTINGS = "smithy.netty.http2-ack-settings";
    // FIXME, clearly define whether this attribute is set on the parent or in the child
    public static final AttributeKey<Http2MultiplexedConnectionPool> HTTP2_MULTIPLEXED_CONNECTION_POOL =
            AttributeKey.valueOf("smithy.netty.http2-multiplexed-connection-pool");
    public static final AttributeKey<ChannelPool> CHANNEL_POOL =
            AttributeKey.valueOf("aws.http.nio.netty.async.ChannelPool");
    public static final AttributeKey<Integer> HTTP2_INITIAL_WINDOW_SIZE =
            AttributeKey.valueOf("smithy.netty.http2-initial-window-size");
    public static final AttributeKey<Http2Connection> HTTP2_CONNECTION =
            AttributeKey.valueOf("smithy.netty.http2-connection");
    public static final AttributeKey<MultiplexedChannel> MULTIPLEXED_CHANNEL = AttributeKey.valueOf(
            "smithy.netty.multiplexed-channel");
    // Channel attributes
    static final AttributeKey<CompletableFuture<HttpVersion>> HTTP_VERSION_FUTURE =
            AttributeKey.valueOf("smithy.netty.http-version");
    static final AttributeKey<Long> MAX_CONCURRENT_STREAMS =
            AttributeKey.valueOf("smithy.netty.max-concurrent-streams");

    private NettyHttp2Constants() {}
}
