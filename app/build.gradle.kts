plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.ai_agent_v1"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ai_agent_v1"
        minSdk = 29
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
        // Cập nhật phiên bản trình biên dịch Compose để tương thích với Kotlin
        kotlinCompilerExtensionVersion = "1.5.10"
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
    // Sử dụng BOM để quản lý tất cả các phiên bản của Compose một cách tự động và tương thích
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Không cần chỉ định phiên bản của compose-compiler nữa khi sử dụng BOM
    // implementation("androidx.compose:compose-compiler:1.5.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    // Sử dụng thư viện Material 3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")

    // Sửa lỗi Unresolved References cho các icon
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // Thư viện cần thiết cho việc ghi âm
    implementation("androidx.media:media:1.7.1")

    // Thư viện OkHttp để thực hiện các yêu cầu HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlin Coroutines để quản lý các tác vụ bất đồng bộ
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
