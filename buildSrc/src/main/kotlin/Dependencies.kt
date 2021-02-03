package org.tiqr.authenticator.buildsrc

private const val kotlinVersion = "1.4.21"
private const val navigationVersion = "2.3.3"
private const val hiltVersion = "2.31.2-alpha"

object Versions {
    // Android
    const val compileSdkVersion = 30
    const val targetSdkVersion = 30
    const val minSdkVersion = 21
    const val buildToolsVersion = "30.0.3"

    // Libs
    const val firebaseCore = "17.2.0"
    const val firebaseMessaging = "20.0.0"
}

object Plugins {
    private val gradleAndroidVersion
        get() = Properties.gradleAndroidPluginVersion ?: "4.1.2"

    val android = "com.android.tools.build:gradle:$gradleAndroidVersion"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val hilt = "com.google.dagger:hilt-android-gradle-plugin:$hiltVersion"
    const val googleServices = "com.google.gms:google-services:4.3.3"

    object Navigation {
        const val safeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:$navigationVersion"
    }
}

object Libs {
    object Kotlin {
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"

        object Coroutines {
            private const val version = "1.4.2"
            const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            const val playServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$version"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object AndroidX {
        const val activity = "androidx.activity:activity-ktx:1.1.0"
        const val appcompat = "androidx.appcompat:appcompat:1.2.0"
        const val fragment = "androidx.fragment:fragment-ktx:1.2.5"
        const val core = "androidx.core:core-ktx:1.3.2"
        const val browser = "androidx.browser:browser:1.3.0"
        const val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
        const val biometric = "androidx.biometric:biometric-ktx:1.2.0-alpha02" //contains important changes
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.4"
        const val futures = "androidx.concurrent:concurrent-futures-ktx:1.1.0"
        const val autofill = "androidx.autofill:autofill:1.1.0"
        const val localBroadcastManager = "androidx.localbroadcastmanager:localbroadcastmanager:1.0.0"

        object Room {
            private const val version = "2.2.6"
            const val runtime = "androidx.room:room-runtime:$version"
            const val ktx = "androidx.room:room-ktx:$version"
            const val compiler = "androidx.room:room-compiler:$version"
            const val sqlite = "androidx.sqlite:sqlite-ktx:2.1.0"
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
            const val fragment = "androidx.navigation:navigation-fragment-ktx:$navigationVersion"
            const val ui = "androidx.navigation:navigation-ui-ktx:$navigationVersion"
        }

        object Camera {
            private const val version = "1.0.0-rc02"
            const val core = "androidx.camera:camera-core:$version"
            const val camera2 = "androidx.camera:camera-camera2:$version"
            const val lifecycle = "androidx.camera:camera-lifecycle:$version"
            const val view = "androidx.camera:camera-view:1.0.0-alpha21"
            const val extensions = "androidx.camera:camera-extensions:1.0.0-alpha21"
        }
    }

    object Google {
        const val material = "com.google.android.material:material:1.2.1"

        object MlKit {
            const val barcode = "com.google.mlkit:barcode-scanning:16.1.1"
        }
    }

    object Testing {
        const val jUnit = "junit:junit:4.12"
        const val jUnitAndroidx = "androidx.test.ext:junit:1.1.1"
        const val epsresso = "androidx.test.espresso:espresso-core:3.2.0"
    }

    object OkHttp {
        private const val version = "4.9.1"
        const val okhttp = "com.squareup.okhttp3:okhttp:$version"
        const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object Retrofit {
        private const val version = "2.9.0"
        const val retrofit = "com.squareup.retrofit2:retrofit:$version"
        const val converterScalars = "com.squareup.retrofit2:converter-scalars:$version"
        const val converterMoshi = "com.squareup.retrofit2:converter-moshi:$version"
    }

    object Moshi {
        private const val version = "1.11.0"
        const val moshi = "com.squareup.moshi:moshi:$version"
        const val adapters = "com.squareup.moshi:moshi-adapters:$version"
        const val codegen = "com.squareup.moshi:moshi-kotlin-codegen:$version"
    }

    object Hilt {
        const val android = "com.google.dagger:hilt-android:$hiltVersion"
        const val compiler = "com.google.dagger:hilt-compiler:$hiltVersion"
        const val fragment = "androidx.hilt:hilt-navigation-fragment:1.0.0-alpha03"
    }

    object Timber {
        private const val version = "4.7.1"
        const val timber = "com.jakewharton.timber:timber:$version"
    }

    object Permission {
        private const val version = "3.1.3"
        const val kPermissions = "com.github.fondesa:kpermissions:$version"
        const val kPermissionsCoroutines = "com.github.fondesa:kpermissions-coroutines:$version"
    }

    object Image {
        const val coil = "io.coil-kt:coil:1.1.1"
    }

    const val betterLink = "me.saket:better-link-movement-method:2.2.0"
}