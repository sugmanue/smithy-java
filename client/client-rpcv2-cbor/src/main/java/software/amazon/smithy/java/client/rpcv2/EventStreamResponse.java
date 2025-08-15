package software.amazon.smithy.java.client.rpcv2;

import software.amazon.smithy.java.aws.events.AwsEventDecoderFactory;
import software.amazon.smithy.java.aws.events.AwsEventFrame;
import software.amazon.smithy.java.core.schema.OutputEventStreamingApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.event.EventDecoderFactory;
import software.amazon.smithy.java.core.serde.event.EventStreamFrameDecodingProcessor;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class EventStreamResponse {

    public <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> deserializeResponse(
            OutputEventStreamingApiOperation<I, O, ?> operation,
            Codec codec,
            HttpResponse response
    ) {
        var eventDecoderFactory = getEventDecoderFactory(operation, codec);
        DataStream bodyDataStream = bodyDataStream(response);
        CompletableFuture<O> result = new CompletableFuture<>();

        var processor = EventStreamFrameDecodingProcessor.create(bodyDataStream, eventDecoderFactory);
        processor.prepare();
        processor.subscribe(new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(1);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onNext(SerializableStruct item) {
                result.complete((O) item);
            }

            @Override
            public void onError(Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
            }
        });

        return result;
    }

    private EventDecoderFactory<AwsEventFrame> getEventDecoderFactory(
            OutputEventStreamingApiOperation<?, ?, ?> outputOperation, Codec codec
    ) {
        return AwsEventDecoderFactory.forOutputStream(outputOperation,
                codec,
                f -> f);
    }

    private DataStream bodyDataStream(HttpResponse response) {
        var contentType = response.headers().contentType();
        var contentLength = response.headers().contentLength();
        return DataStream.withMetadata(response.body(), contentType, contentLength, null);
    }
}
