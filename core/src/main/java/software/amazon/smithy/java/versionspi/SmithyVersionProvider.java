/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.versionspi;

/**
 * SPI for reporting the version of a Smithy Java module.
 *
 * <p>Each Smithy Java module provides an implementation of this interface via
 * {@link java.util.ServiceLoader}. Generated clients use these providers to
 * validate that all Smithy Java modules on the classpath have compatible versions.
 */
public interface SmithyVersionProvider {
    /**
     * Returns the module version information.
     */
    ModuleVersion getModuleVersion();
}
