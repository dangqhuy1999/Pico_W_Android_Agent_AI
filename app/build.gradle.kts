plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ai_agent_v1"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ai_agent_v1"
        minSdk = 33
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Cập nhật để sử dụng thư viện Material 3
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.2")

    // THÊM DÒNG NÀY ĐỂ KHẮC PHỤC LỖI UNRESOLVED REFERENCES CHO CÁC ICON
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Thư viện cần thiết cho việc ghi âm
    implementation("androidx.media:media:1.6.0")

    // Thư viện OkHttp để thực hiện các yêu cầu HTTP
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Kotlin Coroutines để quản lý các tác vụ bất đồng bộ
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
}
