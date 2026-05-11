/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.smithy.java.versionspi.ModuleVersion;

/**
 * Measures the startup cost of the version compatibility check.
 *
 * <p>In production, {@code VersionCheck.check()} runs exactly once during client
 * construction. This benchmark measures the per-invocation cost to quantify
 * the one-time startup impact.
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class VersionCheckBench {

    private static final ModuleVersion CODEGEN_VERSION = new ModuleVersion("benchmark", 1, 1, 0);

    @Setup(org.openjdk.jmh.annotations.Level.Trial)
    @SuppressFBWarnings(value = "LG_LOST_LOGGER_DUE_TO_WEAK_REFERENCE", justification = "Intentional for benchmark")
    public void setupTrial() {
        Logger.getLogger(VersionCheck.class.getName()).setLevel(Level.OFF);
    }

    @Setup(org.openjdk.jmh.annotations.Level.Invocation)
    public void setupInvocation() {
        VersionCheck.reset();
    }

    @Benchmark
    public void versionCheckEnabled() {
        VersionCheck.check(CODEGEN_VERSION);
    }

    @Benchmark
    public void versionCheckSkipped() {
        System.setProperty("smithy.java.skipVersionCheck", "true");
        try {
            VersionCheck.check(CODEGEN_VERSION);
        } finally {
            System.clearProperty("smithy.java.skipVersionCheck");
        }
    }
}
