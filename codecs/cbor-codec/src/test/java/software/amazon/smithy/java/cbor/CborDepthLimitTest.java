/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.cbor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.serde.SerializationException;

class CborDepthLimitTest {

    private static final int MAX_DEPTH = 1000;

    // CBOR indefinite-length array: 0x9F start, 0xFF break
    // CBOR indefinite-length map: 0xBF start, 0xFF break
    // CBOR single-byte text string "a": 0x61 0x61

    private static byte[] nestedArrays(int depth) {
        // Each level: 0x9F (start array), innermost: 0x01 (integer 1), then depth x 0xFF (breaks)
        byte[] payload = new byte[depth + 1 + depth];
        for (int i = 0; i < depth; i++) {
            payload[i] = (byte) 0x9F;
        }
        payload[depth] = 0x01; // integer 1 at the bottom
        for (int i = 0; i < depth; i++) {
            payload[depth + 1 + i] = (byte) 0xFF;
        }
        return payload;
    }

    private static byte[] nestedMaps(int depth) {
        // Each level: 0xBF (start map) + key "a" (0x61 0x61), innermost: 0x01, then depth x 0xFF
        // Structure: {a: {a: {a: ... 1 ...}}}
        byte[] payload = new byte[depth * 3 + 1 + depth];
        int pos = 0;
        for (int i = 0; i < depth; i++) {
            payload[pos++] = (byte) 0xBF; // start map
            payload[pos++] = 0x61; // text string length 1
            payload[pos++] = 0x61; // 'a'
        }
        payload[pos++] = 0x01; // integer 1 as the innermost value
        for (int i = 0; i < depth; i++) {
            payload[pos++] = (byte) 0xFF; // break
        }
        return payload;
    }

    @Test
    void nestedArraysAtLimitSucceeds() {
        byte[] payload = nestedArrays(MAX_DEPTH);
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertNotNull(de.readDocument());
    }

    @Test
    void nestedArraysOverLimitThrows() {
        byte[] payload = nestedArrays(MAX_DEPTH + 1);
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertThrows(SerializationException.class, de::readDocument);
    }

    @Test
    void nestedMapsAtLimitSucceeds() {
        byte[] payload = nestedMaps(MAX_DEPTH);
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertNotNull(de.readDocument());
    }

    @Test
    void nestedMapsOverLimitThrows() {
        byte[] payload = nestedMaps(MAX_DEPTH + 1);
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertThrows(SerializationException.class, de::readDocument);
    }

    @Test
    void siblingContainersDoNotAccumulateDepth() {
        // A flat array containing many empty nested arrays should not trigger the limit
        // Structure: [ [], [], [], ... ] with MAX_DEPTH + 10 siblings
        int siblings = MAX_DEPTH + 10;
        // outer 0x9F + siblings * (0x9F 0xFF) + outer 0xFF
        byte[] payload = new byte[1 + siblings * 2 + 1];
        int pos = 0;
        payload[pos++] = (byte) 0x9F; // start outer array
        for (int i = 0; i < siblings; i++) {
            payload[pos++] = (byte) 0x9F; // start inner array
            payload[pos++] = (byte) 0xFF; // end inner array
        }
        payload[pos] = (byte) 0xFF; // end outer array
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertNotNull(de.readDocument());
    }

    @Test
    void shallowPayloadSucceeds() {
        // {"a": [1, 2, 3]}
        byte[] payload = {
                (byte) 0xBF, // start map
                0x61,
                0x61, // key "a"
                (byte) 0x9F, // start array
                0x01,
                0x02,
                0x03, // integers 1, 2, 3
                (byte) 0xFF, // end array
                (byte) 0xFF // end map
        };
        var de = Rpcv2CborCodec.builder().build().createDeserializer(payload);
        assertNotNull(de.readDocument());
    }
}
