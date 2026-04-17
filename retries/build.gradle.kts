plugins {
    id("smithy-java.module-conventions")
}

description = "This module provides the Smithy Java Retries implementation"

extra["displayName"] = "Smithy :: Java :: Retries"
extra["moduleName"] = "software.amazon.smithy.java.retries"

dependencies {
    implementation(libs.smithy.utils)
    implementation(project(":logging"))
    api(project(":retries-api"))
}
