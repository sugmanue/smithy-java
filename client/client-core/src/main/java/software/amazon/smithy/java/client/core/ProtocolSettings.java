/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.client.core;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Settings used to instantiate a {@link ClientProtocol} implementation.
 */
public final class ProtocolSettings {
    private final ShapeId service;
    private final String serviceVersion;
    private final Trait[] serviceTraits;

    private ProtocolSettings(Builder builder) {
        this.service = builder.service;
        this.serviceVersion = builder.serviceVersion;
        this.serviceTraits = builder.serviceTraits.toArray(new Trait[0]);
    }

    public ShapeId service() {
        return service;
    }

    /**
     * Gets the service version string.
     *
     * <p>The service version is required by some protocols (e.g., AWS Query)
     * that include the version in the request body.
     *
     * @return the service version, or null if not set
     */
    public String serviceVersion() {
        return serviceVersion;
    }

    /**
     * Gets a service-level trait by its {@link TraitKey}.
     *
     * <p>Service-level traits are traits applied to the service shape that
     * protocols may need at runtime (e.g., {@link TraitKey#XML_NAMESPACE_TRAIT}).
     *
     * @param key the trait key to look up
     * @param <T> the trait type
     * @return the trait instance, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T extends Trait> T getServiceTrait(TraitKey<T> key) {
        var traitClass = key.traitClass();
        for (var trait : serviceTraits) {
            if (traitClass.isInstance(trait)) {
                return (T) trait;
            }
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ShapeId service;
        private String serviceVersion;
        private final List<Trait> serviceTraits = new ArrayList<>();

        private Builder() {}

        public Builder service(ShapeId service) {
            this.service = service;
            return this;
        }

        /**
         * Sets the service version string.
         *
         * @param serviceVersion the service version
         * @return the builder
         */
        public Builder serviceVersion(String serviceVersion) {
            this.serviceVersion = serviceVersion;
            return this;
        }

        /**
         * Adds a service-level trait that protocols may need at runtime.
         *
         * @param trait the trait to add
         * @return the builder
         */
        public Builder putServiceTrait(Trait trait) {
            this.serviceTraits.add(trait);
            return this;
        }

        public ProtocolSettings build() {
            return new ProtocolSettings(this);
        }
    }
}
