/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.cli;

import picocli.CommandLine;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class SmithyCallRunner {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SmithyCall())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);

        System.exit(exitCode);
    }
}
