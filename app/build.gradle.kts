plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.jitendersingh.friendsengineer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jitendersingh.friendsengineer"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/LICENSE.txt")
            excludes.add("META-INF/NOTICE")
            excludes.add("META-INF/NOTICE.txt")
            excludes.add("META-INF/*.SF")
            excludes.add("META-INF/*.DSA")
            excludes.add("META-INF/*.RSA")
        }
    }

    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk15to18:1.72")
            force("org.bouncycastle:bcpkix-jdk15to18:1.72")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Apache POI — only once, latest version
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage:21.0.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // iText 7
    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
    }
}