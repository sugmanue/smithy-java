plugins {
    id("smithy-java.module-conventions")
    id("smithy-java.jmh-conventions")
}

description = "This module provides AWS event streaming support"

extra["displayName"] = "Smithy :: Java :: AWS :: Event Streams"
extra["moduleName"] = "software.amazon.smithy.java.aws.events"

tasks.test {
    dependsOn(":codecs:json-codec:shadowJar")
}

dependencies {
    api(project(":core"))
    implementation(project(":logging"))
    api("software.amazon.eventstream:eventstream:1.0.1")
    testImplementation(project(":codecs:json-codec"))

    // Benchmarks reuse the hand-written event-stream fixtures in src/test
    // (TestOperation, TestEventStream, and the per-event structs) and drive
    // the package-private encoder/decoder directly, so the jmh source set
    // depends on the test source set's output and classpath.
    jmhImplementation(project(":codecs:json-codec"))
    jmhImplementation(sourceSets["test"].output)
    jmhImplementation(sourceSets["test"].runtimeClasspath)
}

tasks.named("compileJmhJava") {
    dependsOn("compileTestJava")
    dependsOn(":codecs:json-codec:shadowJar")
}
