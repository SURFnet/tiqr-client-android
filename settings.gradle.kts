rootProject.name = "tiqr client android"

include(":app")
include(":data")
include(":core")

// Enable Gradle's version catalog support
// https://docs.gradle.org/current/userguide/platforms.html
enableFeaturePreview("VERSION_CATALOGS")