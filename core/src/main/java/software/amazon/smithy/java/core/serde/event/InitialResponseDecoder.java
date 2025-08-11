package software.amazon.smithy.java.core.serde.event;

import software.amazon.smithy.java.core.schema.SerializableStruct;

import java.util.concurrent.Flow;

public interface InitialResponseDecoder<F extends Frame<?>> {

    SerializableStruct decode(F frame, Flow.Publisher<? extends SerializableStruct> publisher);

}
