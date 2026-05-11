plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.simats.Tmapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.simats.Tmapp"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // Retrofit and Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // OkHttp for Multipart/File handling
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Agora SDK
    implementation("io.agora.rtc:full-sdk:4.2.2")

    // Socket.IO
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }

    // Flexbox Layout
    implementation(libs.flexbox)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glideCompiler)

    // PDF Viewer
    implementation(libs.pdf.viewer)

    // MPAndroidChart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Razorpay Integration
    implementation("com.razorpay:checkout:1.6.33") {
        exclude(group = "com.razorpay", module = "standard-core")
        exclude(group = "com.razorpay", module = "core")
    }
}