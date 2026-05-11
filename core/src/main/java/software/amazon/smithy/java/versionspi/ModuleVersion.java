/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.versionspi;

/**
 * Represents the version of a Smithy Java module.
 *
 * @param moduleName the module name, e.g. {@code "software.amazon.smithy.java.core"}
 * @param major the major version component
 * @param minor the minor version component
 * @param patch the patch version component
 */
public record ModuleVersion(String moduleName, int major, int minor, int patch) implements Comparable<ModuleVersion> {

    @Override
    public int compareTo(ModuleVersion other) {
        int c = Integer.compare(major, other.major);
        if (c != 0) return c;
        c = Integer.compare(minor, other.minor);
        if (c != 0) return c;
        return Integer.compare(patch, other.patch);
    }

    /**
     * Returns the version as a string, e.g. {@code "1.2.3"}.
     */
    public String versionString() {
        return major + "." + minor + "." + patch;
    }

    @Override
    public String toString() {
        return moduleName + "=" + versionString();
    }
}
