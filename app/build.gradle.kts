import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// 发布签名材料从 keystore.properties 读取（文件缺失时退回无签名构建，不阻塞日常调试）
// 注意：这里必须 import Properties，因为 Kotlin DSL 里 `java` 会被解析成 java 插件扩展，
// 直接写 java.util.Properties 会报 Unresolved reference: util。
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) {
        f.inputStream().use { stream -> load(stream) }
    }
}

android {
    namespace = "com.ziweistar.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ziweistar.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "3.0.0"
        vectorDrawables { useSupportLibrary = true }
        // 仅 debug 变体放行截图，用于开发期视觉走查；release 恒为 false
        buildConfigField("boolean", "SCREENSHOT_ALLOWED", "true")
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "SCREENSHOT_ALLOWED", "true")
        }
        release {
            // 正式分发包：必须签名才能直接安装到手机
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            buildConfigField("boolean", "SCREENSHOT_ALLOWED", "false")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    lint {
        abortOnError = false
        // 离线构建环境里 lintVital 需要额外下载 com.android.tools.lint:lint-gradle，
        // 缓存缺失会让 assembleRelease 直接失败；发布包的正确性由单元测试与真机走查保证。
        checkReleaseBuilds = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Room + SQLCipher (encrypted local vault)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("net.zetetic:sqlcipher-android:4.6.1")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Serialization / networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Lunar calendar engine (寿星万年历算法实现)
    implementation(files("libs/lunar-1.7.4.jar"))

    // Images
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")
}
