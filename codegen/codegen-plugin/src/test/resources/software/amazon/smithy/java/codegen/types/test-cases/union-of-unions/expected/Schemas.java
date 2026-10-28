package software.amazon.smithy.java.example.standalone.model;

import software.amazon.smithy.java.core.schema.PreludeSchemas;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines schemas for shapes in the model package.
 */
final class Schemas {
    static final SchemaBuilder DEEPEST_UNION_BUILDER = Schema.unionBuilder(ShapeId.from("smithy.test#DeepestUnion"));
    static final SchemaBuilder INNER_UNION_BUILDER = Schema.unionBuilder(ShapeId.from("smithy.test#InnerUnion"));
    static final SchemaBuilder OUTER_UNION_BUILDER = Schema.unionBuilder(ShapeId.from("smithy.test#OuterUnion"));

    static {
            DEEPEST_UNION_BUILDER
                .putMember("outer", Schemas.OUTER_UNION_BUILDER)
                .putMember("text", PreludeSchemas.STRING)
                .builderSupplier(DeepestUnion::builder)
                .shapeClass(DeepestUnion.class)
                .build();

            INNER_UNION_BUILDER
                .putMember("deepest", Schemas.DEEPEST_UNION_BUILDER)
                .putMember("number", PreludeSchemas.INTEGER)
                .builderSupplier(InnerUnion::builder)
                .shapeClass(InnerUnion.class)
                .build();

            OUTER_UNION_BUILDER
                .putMember("inner", Schemas.INNER_UNION_BUILDER)
                .putMember("other", PreludeSchemas.STRING)
                .builderSupplier(OuterUnion::builder)
                .shapeClass(OuterUnion.class)
                .build();

    }

    static final Schema DEEPEST_UNION = DEEPEST_UNION_BUILDER.build().resolve();
    static final Schema INNER_UNION = INNER_UNION_BUILDER.build().resolve();
    static final Schema OUTER_UNION = OUTER_UNION_BUILDER.build().resolve();

    private Schemas() {}
}
