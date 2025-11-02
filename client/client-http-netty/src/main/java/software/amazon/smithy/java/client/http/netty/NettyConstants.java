/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty;

import io.netty.channel.pool.ChannelPool;
import io.netty.util.AttributeKey;
import java.util.concurrent.CompletableFuture;
import software.amazon.smithy.java.http.api.HttpVersion;

/**
 * Constant values used in the Netty HTTP client.
 */
public final class NettyConstants {

    // Handler names
    public static final String SSL = "smithy.netty.ssl";
    public static final String SSL_HANDSHAKE = "smithy.netty.ssl-handshake";
    public static final String SSL_CLOSE_COMPLETE = "smithy.netty.ssl-close-complete";
    public static final String HTTP11_CODEC = "smithy.netty.http1_1";
    public static final String PROTOCOL_NEGOTIATION = "smithy.netty.protocol-negotiation";

    // Channel attributes
    static final AttributeKey<CompletableFuture<HttpVersion>> HTTP_VERSION_FUTURE =
            AttributeKey.valueOf("smithy.netty.http-version");
    public static final AttributeKey<ChannelPool> CHANNEL_POOL =
            AttributeKey.valueOf("aws.http.nio.netty.async.ChannelPool");

    private NettyConstants() {}
}
