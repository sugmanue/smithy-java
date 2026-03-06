/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.aws.events.model;

import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas {
    static final Schema BLOB_EVENT = Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#BlobEvent"))
            .putMember("payload",
                    PreludeSchemas.BLOB,
                    new EventPayloadTrait())
            .builderSupplier(BlobEvent::builder)
            .build();

    static final Schema BODY_AND_HEADER_EVENT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#BodyAndHeaderEvent"))
                    .putMember("intMember",
                            PreludeSchemas.INTEGER,
                            new EventHeaderTrait())
                    .putMember("stringMember", PreludeSchemas.STRING)
                    .builderSupplier(BodyAndHeaderEvent::builder)
                    .build();

    static final Schema HEADERS_ONLY_EVENT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#HeadersOnlyEvent"))
                    .putMember("sequenceNum",
                            PreludeSchemas.INTEGER,
                            new EventHeaderTrait())
                    .builderSupplier(HeadersOnlyEvent::builder)
                    .build();

    static final Schema STRING_EVENT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#StringEvent"))
                    .putMember("payload",
                            PreludeSchemas.STRING,
                            new EventPayloadTrait())
                    .builderSupplier(StringEvent::builder)
                    .build();

    static final Schema STRUCTURE_EVENT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#StructureEvent"))
                    .putMember("foo", PreludeSchemas.STRING)
                    .builderSupplier(StructureEvent::builder)
                    .build();

    static final Schema TEST_EVENT_STREAM = Schema
            .unionBuilder(ShapeId.from("smithy.example.eventstreaming#TestEventStream"),
                    new StreamingTrait())
            .putMember("structureMember", Schemas.STRUCTURE_EVENT)
            .putMember("stringMember", Schemas.STRING_EVENT)
            .putMember("blobMember", Schemas.BLOB_EVENT)
            .putMember("headersOnlyMember", Schemas.HEADERS_ONLY_EVENT)
            .putMember("bodyAndHeaderMember", Schemas.BODY_AND_HEADER_EVENT)
            .builderSupplier(TestEventStream::builder)
            .build();

    static final Schema TEST_OPERATION_INPUT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#TestOperationInput"))
                    .putMember("headerString",
                            PreludeSchemas.STRING,
                            new EventHeaderTrait())
                    .putMember("inputStringMember", PreludeSchemas.STRING)
                    .putMember("stream", Schemas.TEST_EVENT_STREAM)
                    .builderSupplier(TestOperationInput::builder)
                    .build();

    static final Schema TEST_OPERATION_OUTPUT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#TestOperationOutput"))
                    .putMember("intMemberHeader",
                            PreludeSchemas.INTEGER,
                            new EventHeaderTrait())
                    .putMember("stringMember", PreludeSchemas.STRING)
                    .putMember("outputStream", Schemas.TEST_EVENT_STREAM)
                    .builderSupplier(TestOperationOutput::builder)
                    .build();

    static final Schema TEST_OPERATION_WITH_EXCEPTION_INPUT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#TestOperationWithExceptionInput"))
                    .putMember("stream", Schemas.TEST_EVENT_STREAM)
                    .builderSupplier(TestOperationWithExceptionInput::builder)
                    .build();

    static final Schema MY_ERROR = Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#MyError"),
            new ErrorTrait("client"))
            .putMember("message", PreludeSchemas.STRING)
            .builderSupplier(MyError::builder)
            .build();

    static final Schema EVENT_STREAM_WITH_ERROR = Schema
            .unionBuilder(ShapeId.from("smithy.example.eventstreaming#EventStreamWithError"),
                    new StreamingTrait())
            .putMember("modeledErrorMember", Schemas.MY_ERROR)
            .putMember("stringMember", Schemas.STRING_EVENT)
            .builderSupplier(EventStreamWithError::builder)
            .build();

    static final Schema TEST_OPERATION_WITH_EXCEPTION_OUTPUT =
            Schema.structureBuilder(ShapeId.from("smithy.example.eventstreaming#TestOperationWithExceptionOutput"))
                    .putMember("outputStream", Schemas.EVENT_STREAM_WITH_ERROR)
                    .builderSupplier(TestOperationWithExceptionOutput::builder)
                    .build();

    private Schemas() {}
}
