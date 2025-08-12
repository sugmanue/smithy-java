package software.amazon.smithy.java.client.rpcv2;

import software.amazon.eventstream.Message;
import software.amazon.smithy.java.aws.events.AwsEventDecoderFactory;
import software.amazon.smithy.java.aws.events.AwsEventFrame;
import software.amazon.smithy.java.core.schema.OutputEventStreamingApiOperation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.Codec;
import software.amazon.smithy.java.core.serde.ShapeDeserializer;
import software.amazon.smithy.java.core.serde.SpecificShapeDeserializer;
import software.amazon.smithy.java.core.serde.event.EventDecoderFactory;
import software.amazon.smithy.java.core.serde.event.EventStreamFrameDecodingProcessor;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class EventStreamResponse {

    public <I extends SerializableStruct, O extends SerializableStruct> CompletableFuture<O> deserializeResponse(
            OutputEventStreamingApiOperation<I, O, ?> operation,
            Codec codec,
            HttpResponse response
    ) {
        var eventDecoderFactory = getEventDecoderFactory(operation, codec);
        DataStream bodyDataStream = bodyDataStream(response);
        CompletableFuture<O> result = new CompletableFuture<>();

        var processor = EventStreamFrameDecodingProcessor.create(
                bodyDataStream, eventDecoderFactory,
                new InitialResponseDecoder<>(operation::outputBuilder, codec)
        );

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

    static class InitialResponseDecoder<E extends SerializableStruct> implements software.amazon.smithy.java.core.serde.event.InitialResponseDecoder<AwsEventFrame> {
        private final Supplier<ShapeBuilder<E>> responseBuilder;
        private final Codec codec;

        public InitialResponseDecoder(
                Supplier<ShapeBuilder<E>> responseBuilder,
                Codec codec
        ) {
            this.responseBuilder = responseBuilder;
            this.codec = codec;
        }

        @Override
        public SerializableStruct decode(AwsEventFrame frame, Flow.Publisher<? extends SerializableStruct> publisher) {
            Message message = frame.unwrap();
            String eventType = getEventType(message);
            if (!"initial-response".equals(eventType)) {
                throw new UnsupportedOperationException("Unsupported frame type: " + eventType);
            }
            var builder = responseBuilder.get();
            var codecDeserializer = codec.createDeserializer(message.getPayload());
            builder.deserialize(codecDeserializer);
            var publisherMember = getPublisherMember(builder.schema());
            var responseDeserializer = new EventStreamDeserializer(publisherMember, publisher);
            builder.deserialize(responseDeserializer);
            return builder.build();
        }

        private Schema getPublisherMember(Schema schema) {
            for (var member : schema.members()) {
                if (member.memberTarget().hasTrait(TraitKey.STREAMING_TRAIT)) {
                    return member;
                }
            }
            throw new IllegalArgumentException("cannot find streaming member");
        }

        private String getEventType(Message message) {
            return message.getHeaders().get(":event-type").getString();
        }
    }

    static class EventStreamDeserializer extends SpecificShapeDeserializer {
        private final Schema publisherMember;
        private final Flow.Publisher<? extends SerializableStruct> publisher;

        EventStreamDeserializer(Schema publisherMember, Flow.Publisher<? extends SerializableStruct> publisher) {
            this.publisherMember = publisherMember;
            this.publisher = publisher;
        }

        @Override
        public Flow.Publisher<? extends SerializableStruct> readEventStream(Schema schema) {
            return publisher;
        }

        @Override
        public <T> void readStruct(Schema schema, T state, ShapeDeserializer.StructMemberConsumer<T> consumer) {
            consumer.accept(state, publisherMember, this);
        }
    }
}
