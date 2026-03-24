plugins {
    alias(libs.plugins.android.application)
}

import java.util.Properties

// 讀取 keystore 配置
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasKeystore = keystorePropertiesFile.exists()

if (hasKeystore) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.example.appwriteandroidtrae"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.appwriteandroidtrae"
        minSdk = 24
        targetSdk = 36
        versionCode = 16
        versionName = "1.0.16"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasKeystore) {
                // 使用 keystore.properties 文件中的配置
                val storeFilePath = keystoreProperties["storeFile"].toString()
                storeFile = if (storeFilePath.startsWith("../")) {
                    rootProject.file(storeFilePath.substring(3))
                } else {
                    file(storeFilePath)
                }
                storePassword = keystoreProperties["storePassword"].toString()
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
            } else {
                // 回退到環境變數配置
                val keystorePath = System.getenv("RELEASE_STORE_FILE")
                if (!keystorePath.isNullOrEmpty()) {
                    storeFile = file(keystorePath)
                    storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                    keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // 自動應用簽名配置
            if (hasKeystore || !System.getenv("RELEASE_STORE_FILE").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    implementation("io.appwrite:sdk-for-android:11.4.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    
    // Core library desugaring for Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
