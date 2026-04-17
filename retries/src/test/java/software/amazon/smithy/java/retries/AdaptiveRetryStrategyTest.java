/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.retries;

import static software.amazon.smithy.java.retries.RetryStrategyTestCommon.*;

import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AdaptiveRetryStrategyTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    public void runTestCase(TestCase testCase) {
        testCase.run(AdaptiveRetryStrategy.builder().build());
    }

    public static Collection<TestCase> parameters() {
        return RetryStrategyTestCommon.parameters();
    }
}
