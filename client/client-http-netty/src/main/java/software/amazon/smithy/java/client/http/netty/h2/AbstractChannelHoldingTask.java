/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.http.netty.h2;

import io.netty.channel.Channel;

abstract class AbstractChannelHoldingTask implements Runnable {
    protected final Channel channel;

    public AbstractChannelHoldingTask(Channel channel) {
        this.channel = channel;
    }

    public abstract void run();
}
