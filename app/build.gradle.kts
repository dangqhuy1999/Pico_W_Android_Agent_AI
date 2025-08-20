import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Khai báo biến properties ở ngoài các khối Android, ngay dưới plugins
val properties = Properties()
val propertiesFile = rootProject.file("local.properties")
if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { properties.load(it) }
}

android {
    namespace = "com.example.ai_agent_v1"
    compileSdk = 34

    // Thêm khối buildFeatures vào đây và không lặp lại
    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    defaultConfig {
        applicationId = "com.example.ai_agent_v1"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Khai báo các biến BuildConfigFields
        buildConfigField(
            "String",
            "ASSEMBLYAI_API_KEY",
            properties.getProperty("ASSEMBLYAI_API_KEY", "default_value")
        )
        buildConfigField(
            "String",
            "ASSEMBLYAI_BASE_URL",
            properties.getProperty("ASSEMBLYAI_BASE_URL", "default_value")
        )
        buildConfigField(
            "String",
            "UPLOAD_API",
            properties.getProperty("UPLOAD_API", "default_value")
        )
        buildConfigField(
            "String",
            "TRANSCRIPT_API",
            properties.getProperty("TRANSCRIPT_API", "default_value")
        )
        buildConfigField(
            "String",
            "AI_AGENT_API",
            properties.getProperty("AI_AGENT_API", "default_value")
        )
        buildConfigField(
            "String",
            "TEXT_TO_SPEECH_API",
            properties.getProperty("TEXT_TO_SPEECH_API", "default_value")
        )
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            properties.getProperty("OPENAI_API_KEY", "default_value")
        )
        buildConfigField(
            "String",
            "PICO_W_IP",
            properties.getProperty("PICO_W_IP", "default_value")
        )
        buildConfigField(
            "String",
            "PICO_W_URL",
            properties.getProperty("PICO_W_URL", "default_value")
        )
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
        // Cập nhật lên Java 17 để tránh cảnh báo
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    implementation("androidx.media:media:1.7.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
