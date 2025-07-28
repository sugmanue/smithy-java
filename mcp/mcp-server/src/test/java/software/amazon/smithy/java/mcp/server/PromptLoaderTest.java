/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.mcp.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class PromptLoaderTest {

    @Test
    public void testLoadPromptsWithNoServices() {
        var prompts = PromptLoader.loadPrompts(Collections.emptyList());
        assertTrue(prompts.isEmpty());
    }
}
