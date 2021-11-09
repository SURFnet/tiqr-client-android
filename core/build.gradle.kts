plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
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
}
