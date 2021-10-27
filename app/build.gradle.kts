plugins {
    id("com.android.application")
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
        applicationId = "org.tiqr.authenticator"
        versionCode = 30
        versionName = "4.0.0"

        minSdk = libs.versions.android.sdk.min.get().toInt()
        targetSdk = libs.versions.android.sdk.target.get().toInt()

        testInstrumentationRunner = "org.tiqr.authenticator.runner.HiltAndroidTestRunner"

        manifestPlaceholders["schemeEnroll"] = project.property("schemeEnroll") as String
        manifestPlaceholders["schemeAuth"] = project.property("schemeAuth") as String

        // only package supported languages
        resourceConfigurations += listOf("en", "da", "de", "es", "fr", "fy", "hr", "ja", "lt", "nl", "no", "ro", "sk", "sl", "sr", "tr" )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kapt {
        correctErrorTypes = true
        useBuildCache = true
        javacOptions {
            option("-Xmaxerrs", 1000)
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    lint {
        isAbortOnError = false
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
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

// Disable analytics
configurations {
    all {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "com.google.firebase", module = "firebase-analytics")
        exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
    }
}

apply {
    plugin("com.google.gms.google-services")
}