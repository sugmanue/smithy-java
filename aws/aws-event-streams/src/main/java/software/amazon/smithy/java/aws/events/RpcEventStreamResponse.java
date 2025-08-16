/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.event.EventDecoderFactory;
import software.amazon.smithy.java.core.serde.event.EventStreamFrameDecodingProcessor;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

public final class RpcEventStreamResponse {

    private RpcEventStreamResponse() {}

    public static <O extends SerializableStruct> CompletableFuture<O> deserializeResponse(
            EventDecoderFactory<AwsEventFrame> eventDecoderFactory,
            HttpResponse response
    ) {
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
                result.completeExceptionally(new RuntimeException("Event stream completed"));
            }
        });

        return result;
    }

    private static DataStream bodyDataStream(HttpResponse response) {
        var contentType = response.headers().contentType();
        var contentLength = response.headers().contentLength();
        return DataStream.withMetadata(response.body(), contentType, contentLength, null);
    }
}
