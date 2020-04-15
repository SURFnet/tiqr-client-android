package org.tiqr.authenticator.buildsrc

private const val kotlinVersion = "1.3.71"

object Versions {
    // Android
    const val compileSdkVersion = 29
    const val targetSdkVersion = 29
    const val minSdkVersion = 21
    const val buildToolsVersion = "29.0.3"

    // Libs
    const val firebaseCore = "17.2.0"
    const val firebaseMessaging = "20.0.0"
}

object Plugins {
    private val gradleAndroidVersion
        get() = Properties.gradleAndroidPluginVersion ?: "3.6.1"

    val android = "com.android.tools.build:gradle:$gradleAndroidVersion"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val googleServices = "com.google.gms:google-services:4.3.3"

    object Navigation {
        const val safeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:2.3.0-alpha03"
    }
}

object Libs {
    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"

        object Coroutines {
            private const val version = "1.3.4"
            const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            const val playServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$version"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object AndroidX {
        const val activity = "androidx.activity:activity-ktx:1.1.0"
        const val appcompat = "androidx.appcompat:appcompat:1.1.0"
        const val fragment = "androidx.fragment:fragment-ktx:1.2.4"
        const val core = "androidx.core:core-ktx:1.2.0"
        const val browser = "androidx.browser:browser:1.2.0"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.0-beta4"
        const val futures = "androidx.concurrent:concurrent-futures-ktx:1.1.0-alpha01"
        const val autofill = "androidx.autofill:autofill:1.0.0"

        object Room {
            private const val version = "2.2.5"
            const val runtime = "androidx.room:room-runtime:$version"
            const val ktx = "androidx.room:room-ktx:$version"
            const val compiler = "androidx.room:room-compiler:$version"
        }

        object Lifecycle {
            private const val version = "2.2.0"
            const val extensions = "androidx.lifecycle:lifecycle-extensions:$version"
            const val commonJ8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
            const val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
        }

        object Navigation {
            private const val version = "2.3.0-alpha04"
            const val fragment = "androidx.navigation:navigation-fragment-ktx:$version"
            const val ui = "androidx.navigation:navigation-ui-ktx:$version"
        }

        object Camera {
            private const val version = "1.0.0-beta02"
            const val core = "androidx.camera:camera-core:$version"
            const val camera2 = "androidx.camera:camera-camera2:$version"
            const val lifecycle = "androidx.camera:camera-lifecycle:$version"
            const val view = "androidx.camera:camera-view:1.0.0-alpha09"
            const val extensions = "androidx.camera:camera-extensions:1.0.0-alpha09"
        }
    }

    object Google {
        const val material = "com.google.android.material:material:1.1.0"

        object Firebase {
            const val vision = "com.google.firebase:firebase-ml-vision:24.0.2"
            const val barcode = "com.google.firebase:firebase-ml-vision-barcode-model:16.1.0"
        }
    }

    object Testing {
        const val jUnit = "junit:junit:4.12"
        const val jUnitAndroidx = "androidx.test.ext:junit:1.1.1"
        const val epsresso = "androidx.test.espresso:espresso-core:3.2.0"
    }

    object OkHttp {
        private const val version = "4.5.0"
        const val okhttp = "com.squareup.okhttp3:okhttp:$version"
        const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object Retrofit {
        private const val version = "2.8.1"
        const val retrofit = "com.squareup.retrofit2:retrofit:$version"
        const val converterScalars = "com.squareup.retrofit2:converter-scalars:$version"
        const val converterMoshi = "com.squareup.retrofit2:converter-moshi:$version"
    }

    object Moshi {
        private const val version = "1.9.2"
        const val moshi = "com.squareup.moshi:moshi:$version"
        const val codegen = "com.squareup.moshi:moshi-kotlin-codegen:$version"
    }

    object Dagger {
        private const val version = "2.27"
        const val dagger = "com.google.dagger:dagger:$version"
        const val android = "com.google.dagger:dagger-android:$version"
        const val androidSupport = "com.google.dagger:dagger-android-support:$version"

        const val compiler = "com.google.dagger:dagger-compiler:$version"
        const val androidProcessor = "com.google.dagger:dagger-android-processor:$version"
    }

    object Timber {
        private const val version = "4.7.1"
        const val timber = "com.jakewharton.timber:timber:$version"
    }

    object Permission {
        const val kPermissions = "com.github.fondesa:kpermissions:2.0.2"
    }

    const val betterLink = "me.saket:better-link-movement-method:2.2.0"
}