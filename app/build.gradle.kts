import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

val localProps =
    Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use(::load)
    }
val defaultBaseUrl: String =
    localProps.getProperty("tolocha.baseUrl", "https://radio.ejemplo.com")

android {
    namespace = "com.izquierdojl.tolocharadio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.izquierdojl.tolocharadio"
        minSdk = 26
        targetSdk = 37
        // Versionado: se resuelve desde env/props para que cada tag vX.Y.Z genere
        // un versionName/versionCode coherente en CI. En local cae a 1.0 / 1.
        // Prioridad: env RELEASE_VERSION/VERSION_CODE > -PversionName/versionCode
        //   > GITHUB_REF_NAME (tag vX.Y.Z) > valor por defecto.
        val releaseVersion: String =
            System.getenv("RELEASE_VERSION")
                ?: (project.findProperty("versionName") as String?)
                ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")?.takeIf {
                    it.matches(Regex("""\d+\.\d+\.\d+.*"""))
                }
                ?: "1.0"
        val releaseCode: Int =
            System.getenv("VERSION_CODE")?.toIntOrNull()
                ?: (project.findProperty("versionCode") as String?)?.toIntOrNull()
                ?: System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
                ?: 1
        versionCode = releaseCode
        versionName = releaseVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TOLOCHA_BASE_URL", "\"$defaultBaseUrl\"")
    }

    signingConfigs {
        // Firma release con keystore propio vía env (CI) o local.properties.
        // En CI el workflow decodifica ANDROID_KEYSTORE_BASE64 a un fichero y
        // exporta ANDROID_KEYSTORE_PATH. En local puedes definir en
        // local.properties: tolocha.keystore.path / .password / .alias / .keyPassword.
        // Si no hay keystore, la build release cae a la clave debug (solo pruebas).
        create("release") {
            val ksPath =
                System.getenv("ANDROID_KEYSTORE_PATH")
                    ?: localProps.getProperty("tolocha.keystore.path")
            val ksPassword =
                System.getenv("ANDROID_KEYSTORE_PASSWORD")
                    ?: localProps.getProperty("tolocha.keystore.password")
            val keyAlias =
                System.getenv("ANDROID_KEY_ALIAS")
                    ?: localProps.getProperty("tolocha.key.alias")
            val keyPassword =
                System.getenv("ANDROID_KEY_PASSWORD")
                    ?: localProps.getProperty("tolocha.key.password")
            if (!ksPath.isNullOrBlank() && rootProject.file(ksPath).exists()) {
                storeFile = rootProject.file(ksPath)
                storePassword = ksPassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            optimization {
                enable = false
            }
            // Si hay keystore configurado usa "release", si no debug (evita APK sin firmar).
            val hasKeystore =
                try {
                    signingConfigs.getByName("release").storeFile?.exists() == true
                } catch (_: Exception) {
                    false
                }
            signingConfig =
                if (hasKeystore) {
                    signingConfigs.getByName("release")
                } else {
                    signingConfigs.getByName("debug")
                }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose (T004)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // DI (plan R: Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Network (contrato /api/v1)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Player (Media3, FR-007)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // Cast (Chromecast integration)
    implementation(libs.media3.cast)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.mediarouter)

    // Storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.coil.compose)
    implementation(libs.process.phoenix)

    // Concurrency
    implementation(libs.kotlinx.coroutines.android)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
    // Deuda preexistente documentada (19 issues estructurales de features
    // anteriores); el gate sigue activo para issues nuevos.
    baseline = rootProject.file("detekt-baseline.xml")
}
