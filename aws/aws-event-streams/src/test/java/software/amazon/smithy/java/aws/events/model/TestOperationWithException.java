/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events.model;

import java.util.List;
import java.util.function.Supplier;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.ApiService;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.core.serde.TypeRegistry;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyGenerated;

@SmithyGenerated
public final class TestOperationWithException
        implements ApiOperation<TestOperationWithExceptionInput, TestOperationWithExceptionOutput> {

    private static final TestOperationWithException $INSTANCE = new TestOperationWithException();

    static final Schema $SCHEMA =
            Schema.createOperation(ShapeId.from("smithy.example.eventstreaming#TestOperationWithException"));

    public static final ShapeId $ID = $SCHEMA.id();

    private static final TypeRegistry TYPE_REGISTRY = TypeRegistry.empty();

    private static final List<ShapeId> SCHEMES = List.of(ShapeId.from("smithy.api#noAuth"));

    private static final Schema INPUT_STREAM_MEMBER = TestOperationWithExceptionInput.$SCHEMA.member("stream");
    private static final Schema OUTPUT_STREAM_MEMBER = TestOperationWithExceptionOutput.$SCHEMA.member("outputStream");

    /**
     * Get an instance of this {@code ApiOperation}.
     *
     * @return An instance of this class.
     */
    public static TestOperationWithException instance() {
        return $INSTANCE;
    }

    private TestOperationWithException() {}

    @Override
    public ShapeBuilder<TestOperationWithExceptionInput> inputBuilder() {
        return TestOperationWithExceptionInput.builder();
    }

    @Override
    public Supplier<ShapeBuilder<? extends SerializableStruct>> inputEventBuilderSupplier() {
        return () -> TestEventStream.builder();
    }

    @Override
    public ShapeBuilder<TestOperationWithExceptionOutput> outputBuilder() {
        return TestOperationWithExceptionOutput.builder();
    }

    @Override
    public Supplier<ShapeBuilder<? extends SerializableStruct>> outputEventBuilderSupplier() {
        return () -> EventStreamWithError.builder();
    }

    @Override
    public Schema schema() {
        return $SCHEMA;
    }

    @Override
    public Schema inputSchema() {
        return TestOperationWithExceptionInput.$SCHEMA;
    }

    @Override
    public Schema outputSchema() {
        return TestOperationWithExceptionOutput.$SCHEMA;
    }

    @Override
    public TypeRegistry errorRegistry() {
        return TYPE_REGISTRY;
    }

    @Override
    public List<Schema> errorSchemas() {
        return List.of();
    }

    @Override
    public List<ShapeId> effectiveAuthSchemes() {
        return SCHEMES;
    }

    @Override
    public Schema inputStreamMember() {
        return INPUT_STREAM_MEMBER;
    }

    @Override
    public Schema outputStreamMember() {
        return OUTPUT_STREAM_MEMBER;
    }

    @Override
    public Schema idempotencyTokenMember() {
        return null;
    }

    @Override
    public ApiService service() {
        return EventStreamingTestServiceApiService.instance();
    }
}
