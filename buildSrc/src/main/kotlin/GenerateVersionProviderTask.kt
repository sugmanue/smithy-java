import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task that generates a SmithyVersionProvider SPI implementation for a module.
 */
abstract class GenerateVersionProviderTask : DefaultTask() {

    @get:Input
    var moduleName: String = ""

    @get:Input
    var moduleVersion: String = ""

    @get:OutputDirectory
    var outputDir: File = project.layout.buildDirectory.dir("generated/version-provider").get().asFile

    companion object {
        private const val PACKAGE = "software.amazon.smithy.java.versionspi"
        private const val INTERFACE_NAME = "SmithyVersionProvider"
        private const val RECORD_NAME = "ModuleVersion"
        private const val IMPL_NAME = "GeneratedVersionProvider"
    }

    @TaskAction
    fun generate() {
        val parts = moduleVersion.split(".")
        val major = parts.getOrElse(0) { "0" }
        val minor = parts.getOrElse(1) { "0" }
        val patch = parts.getOrElse(2) { "0" }.replace(Regex("[^0-9].*"), "")

        val packageDir = File(outputDir, "java/${PACKAGE.replace('.', '/')}")
        packageDir.mkdirs()

        // Generate the implementation
        File(packageDir, "$IMPL_NAME.java").writeText(
            """
            |package $PACKAGE;
            |
            |public final class $IMPL_NAME implements $INTERFACE_NAME {
            |    private static final $RECORD_NAME VERSION = new $RECORD_NAME("$moduleName", $major, $minor, $patch);
            |
            |    @Override
            |    public $RECORD_NAME getModuleVersion() {
            |        return VERSION;
            |    }
            |}
            """.trimMargin()
        )

        // Generate META-INF/services file
        val servicesDir = File(outputDir, "resources/META-INF/services")
        servicesDir.mkdirs()
        File(servicesDir, "$PACKAGE.$INTERFACE_NAME").writeText(
            "$PACKAGE.$IMPL_NAME\n"
        )
    }
}
