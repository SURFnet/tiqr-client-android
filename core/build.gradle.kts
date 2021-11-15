import java.util.Properties

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
    `maven-publish`
    signing
}

android {
    compileSdk = libs.versions.android.sdk.compile.get().toInt()
    buildToolsVersion = libs.versions.android.buildTools.get()

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
        targetSdk = libs.versions.android.sdk.target.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8

        }
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

fun loadCustomProperties(file: File): java.util.Properties {
    val properties = Properties()
    if (file.isFile) {
        properties.load(file.inputStream())
    }
    return properties
}

val secureProperties = loadCustomProperties(file("../local.properties"))

    dependencies {
        implementation(libs.kotlin.stdlib)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.androidx.core)
        implementation(libs.kotlinx.coroutines.playServices)

        implementation(libs.androidx.activity)
        implementation(libs.androidx.autofill)
        implementation(libs.androidx.biometric)
        implementation(libs.androidx.browser)
        implementation(libs.androidx.camera.core)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle)
        implementation(libs.androidx.camera.view)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.core)
        implementation(libs.androidx.concurrent)
        implementation(libs.androidx.lifecycle.common)
        implementation(libs.androidx.lifecycle.livedata)
        implementation(libs.androidx.localBroadcastManager)
        implementation(libs.androidx.navigation.fragment)
        implementation(libs.androidx.navigation.ui)
        implementation(libs.androidx.recyclerview)
        implementation(libs.androidx.splashscreen)
        implementation(libs.google.android.material)
        implementation(libs.google.mlkit.barcode)
        implementation(libs.google.firebase.messaging)

        implementation(project(":data"))

        implementation(libs.dagger.hilt.android)
        implementation(libs.dagger.hilt.fragment)
        kapt(libs.dagger.hilt.compiler)

        implementation(libs.permission)
        implementation(libs.coil)
        implementation(libs.betterLink)

        testImplementation(libs.junit)
        androidTestImplementation(libs.junit)
        androidTestImplementation(libs.androidx.testing.core)
        androidTestImplementation(libs.androidx.testing.junit)
        androidTestImplementation(libs.androidx.testing.rules)
        androidTestImplementation(libs.androidx.testing.epsresso)
        androidTestImplementation(libs.androidx.testing.uiautomator)
        androidTestImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.dagger.hilt.testing)
    kaptAndroidTest(libs.dagger.hilt.compiler)
}

group = "org.tiqr"
version = "0.0.17-SNAPSHOT"

tasks {
    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets.getByName("main").java.srcDirs)
    }
}
publishing {
    publications {
        register<MavenPublication>("mavenAndroid") {
            artifactId = "core"

            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
            artifact(tasks.getByName("sourcesJar"))

            pom {
                name.set("core")
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