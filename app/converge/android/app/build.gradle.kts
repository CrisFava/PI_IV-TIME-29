import java.util.Properties

plugins {
    id("com.android.application")
    // START: FlutterFire Configuration
    id("com.google.gms.google-services")
    // END: FlutterFire Configuration
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Lê de forma segura a chave do Google Maps do .env sem expor no Git
val envFile = project.rootProject.file("../.env")
val envProps = Properties()
if (envFile.exists()) {
    envFile.forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
            val parts = trimmed.split("=", limit = 2)
            envProps.setProperty(parts[0].trim(), parts[1].trim())
        }
    }
}
val mapsApiKey = envProps.getProperty("GOOGLE_MAPS_API_KEY")
    ?: System.getenv("GOOGLE_MAPS_API_KEY")
    ?: ""

android {
    namespace = "br.edu.puccampinas.converge"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "br.edu.puccampinas.converge"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // Injeta chave dinamicamente via placeholder
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

// Configura automaticamente `adb reverse tcp:8080 tcp:8080` ao compilar o app Android
// para que `http://localhost:8080` funcione em qualquer dispositivo ou emulador conectado.
tasks.register("adbReverseBackendPort") {
    doLast {
        try {
            val adb = android.adbExecutable
            if (adb.exists()) {
                ProcessBuilder(adb.absolutePath, "reverse", "tcp:8080", "tcp:8080")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor()
            }
        } catch (_: Exception) {
             // Ignora silenciosamente se nenhum dispositivo estiver conectado no momento do build
        }
    }
}

tasks.matching { it.name.startsWith("assemble") || it.name.startsWith("install") }.configureEach {
    finalizedBy("adbReverseBackendPort")
}
