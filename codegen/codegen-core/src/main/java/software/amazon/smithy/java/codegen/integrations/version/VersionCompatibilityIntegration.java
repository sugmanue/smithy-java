/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.java.codegen.integrations.version;

import software.amazon.smithy.java.codegen.CodeGenerationContext;
import software.amazon.smithy.java.codegen.JavaCodegenIntegration;
import software.amazon.smithy.java.core.Version;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Codegen integration that generates a test to validate that all Smithy Java
 * modules on the classpath have compatible versions.
 *
 * <p>Each Smithy Java module includes a resource at
 * {@code META-INF/smithy-java/versions.properties} containing its module name
 * and version. Since each JAR has its own copy, {@code ClassLoader.getResources()}
 * returns one URL per module. The generated test reads all of them and asserts:
 * <ol>
 *   <li>All modules report the same version (strict equality).</li>
 *   <li>All module versions are &ge; the version the code was generated against.</li>
 * </ol>
 */
@SmithyInternalApi
public final class VersionCompatibilityIntegration implements JavaCodegenIntegration {

    private static final String CLASS_NAME = "SmithyVersionCompatibilityTest";
    private static final String TEMPLATE = IoUtils.readUtf8Resource(
            VersionCompatibilityIntegration.class,
            CLASS_NAME + ".java.template");

    @Override
    public String name() {
        return "version-compatibility";
    }

    @Override
    public void customize(CodeGenerationContext codegenContext) {
        var settings = codegenContext.settings();
        var packagePath = settings.packageNamespace().replace(".", "/");
        var testFilePath = "test-java/" + packagePath + "/" + CLASS_NAME + ".java";

        var content = TEMPLATE
                .replace("${VERSION}", Version.VERSION);

        codegenContext.writerDelegator()
                .useFileWriter(testFilePath,
                        settings.packageNamespace(),
                        writer -> writer.writeInlineWithNoFormatting(content));
    }
}
