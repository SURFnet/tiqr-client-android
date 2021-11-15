import java.util.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    `maven-publish`
    signing
}

val secureProperties = loadCustomProperties(file("../local.properties"))

fun loadCustomProperties(file: File): java.util.Properties {
    val properties = Properties()
    if (file.isFile) {
        properties.load(file.inputStream())
    }
    return properties
}

android {
    compileSdk = libs.versions.android.sdk.compile.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
        targetSdk = libs.versions.android.sdk.target.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField("String", "TOKEN_EXCHANGE_BASE_URL", "\"https://tx.tiqr.org/\"")

        buildConfigField("String", "BASE_URL", "\"https://demo.tiqr.org\"")
        buildConfigField("int", "PROTOCOL_VERSION", "2")
        buildConfigField("boolean", "PROTOCOL_COMPATIBILITY_MODE", "true")

        buildConfigField("String", "TIQR_ENROLL_SCHEME", "\"${project.property("schemeEnroll") as String}://\"")
        buildConfigField("String", "TIQR_AUTH_SCHEME", "\"${project.property("schemeAuth") as String}://\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        buildFeatures {
            dataBinding = true
        }

        kapt {
            correctErrorTypes = true
            useBuildCache = true

            javacOptions {
                option("-Xmaxerrs", 1000)
            }

            arguments {
                arg("room.schemaLocation", "$projectDir/schemas".toString())
                arg("room.incremental", "true")
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8

        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.androidx.core)
        implementation(libs.androidx.lifecycle.livedata)
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.google.android.material)

        implementation(libs.dagger.hilt.android)
        kapt(libs.dagger.hilt.compiler)

        api(libs.okhttp.okhttp)
        api(libs.okhttp.logging)

        api(libs.retrofit.retrofit)
        implementation(libs.retrofit.converter.moshi)
        implementation(libs.retrofit.converter.scalars)

        api(libs.moshi.moshi)
        kapt(libs.moshi.codegen)

        api(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        implementation(libs.androidx.room.sqlite)
        kapt(libs.androidx.room.compiler)

        api(libs.timber)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.testing.junit)
        androidTestImplementation(libs.androidx.testing.epsresso)
        androidTestImplementation(libs.kotlinx.coroutines.test)
    }
}

group = "org.tiqr"
version = "0.0.5-SNAPSHOT"

tasks {
    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }
}
publishing {
    publications {
        register<MavenPublication>("mavenAndroid") {
            artifactId = "data"

            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
            artifact(tasks.getByName("sourcesJar"))

            pom {
                name.set("data")
                url.set("https://github.com/SURFnet/tiqr-app-core-android")
                description.set("refactoring original tiqr project")
                developers {
                    developer {
                        name.set("sara hachem")
                        email.set("sara@egeniq.com")
                    }
                    developer {
                        name.set("Dmitry Kovalenko")
                        email.set("dima@egeniq.com")
                    }
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                scm {
                    connection.set("https://github.com/SURFnet/tiqr-app-core-android.git")
                    url.set("https://github.com/SURFnet/tiqr-app-core-android")
                }

                withXml {
                    fun groovy.util.Node.addDependency(dependency: Dependency, scope: String) {
                        appendNode("dependency").apply {
                            appendNode("groupId", dependency.group)
                            appendNode("artifactId", dependency.name)
                            appendNode("version", dependency.version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").let { dependencies ->
                        // List all "api" dependencies as "compile" dependencies
                        configurations.api.get().allDependencies.forEach {
                            dependencies.addDependency(it, "compile")
                        }
                        // List all "implementation" dependencies as "runtime" dependencies
                        configurations.implementation.get().allDependencies.forEach {
                            dependencies.addDependency(it, "runtime")
                        }
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotRepo else releasesRepoUrl)
            credentials {
                username = secureProperties.getProperty("USERNAME")
                password =secureProperties.getProperty("PASSWORD")
            }
        }
    }


    signing {
        useGpgCmd()
        sign(publishing.publications["mavenAndroid"])
    }
}
