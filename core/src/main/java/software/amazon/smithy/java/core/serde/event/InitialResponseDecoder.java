/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.core.serde.event;

import java.util.concurrent.Flow;
import software.amazon.smithy.java.core.schema.SerializableStruct;

public interface InitialResponseDecoder<F extends Frame<?>> {

    SerializableStruct decode(F frame, Flow.Publisher<? extends SerializableStruct> publisher);

}
