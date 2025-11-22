plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.example.lotteryevent"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lotteryevent"
        minSdk = 24
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
    }
}

dependencies {
    // --- Default and Firebase Dependencies ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(platform(libs.firebase.bom))

    // --- Navigation Dependencies ---
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // --- Other Dependencies ---
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.google.guava)
    implementation(libs.zxing.core)

    // --- Testing Dependencies ---
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.fragment.testing)
    debugImplementation(libs.fragment.testing.manifest)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    androidTestImplementation ("org.mockito:mockito-android:5.11.0")
    testImplementation("org.mockito:mockito-core:5.11.0")


    // --- Camera Dependencies ---
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // --- Map Dependencies ---
    implementation(libs.play.services.maps)
}