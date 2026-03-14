/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.mcp.server;

import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface ToolFilter {

    boolean allowTool(String mcpServerName, String toolName);
}
