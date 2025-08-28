/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

/**
 * Initial event types, either a request or a response.
 */
public enum InitialEventType {
    INITIAL_REQUEST("initial-request"),
    INITIAL_RESPONSE("initial-response");

    private final String name;

    InitialEventType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
