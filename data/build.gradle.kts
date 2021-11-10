plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
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
