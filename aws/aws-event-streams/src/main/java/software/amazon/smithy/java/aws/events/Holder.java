/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

/**
 * A mutable container for a single value, intended to be used as an out-parameter
 * that can be passed through multiple layers so that any layer can set or read the value.
 *
 * <p>This class is not thread-safe.
 *
 * @param <T> the type of the held value
 */
final class Holder<T> {
    private T value;

    /**
     * Creates an empty holder with no initial value.
     */
    Holder() {}

    /**
     * Creates a holder with the given initial value.
     *
     * @param value the initial value
     */
    Holder(T value) {
        this.value = value;
    }

    /**
     * Sets the held value, replacing any previously set value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Sets the held value only if no value has been set yet (i.e., the current value is {@code null}).
     *
     * @param value the value to set
     * @return {@code true} if the value was set, {@code false} if a value was already present
     */
    public boolean setIfAbsent(T value) {
        if (this.value == null) {
            this.value = value;
            return true;
        }
        return false;
    }

    /**
     * Returns the currently held value, or {@code null} if no value has been set.
     *
     * @return the held value, or {@code null}
     */
    public T get() {
        return value;
    }
}
