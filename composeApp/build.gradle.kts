import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            // Compose UI
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)

            // Lifecycle & ViewModel
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Navigation
            implementation(libs.navigation.compose)

            // SQLite driver (BundledSQLiteDriver) is provided by each platform source set

            // Serialization (needed for type-safe navigation routes)
            implementation(libs.kotlinx.serialization.json)

            // Date/time utilities
            implementation(libs.kotlinx.datetime)

            // Supabase (Google sign-in + cloud sync/backup)
            implementation(libs.supabase.kt)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.compose.auth)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.darwin)
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.sqlite.bundled)
            implementation(libs.mp3spi) // JVM MP3 decoding for javax.sound playback
            implementation(libs.ktor.client.cio)
        }

        val desktopTest by getting
        desktopTest.dependencies {
            implementation(libs.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.dillu.quranlearner"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.dillu.quranlearner"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true // required by supabase-kt below minSdk 26
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// ─────────────────────────────────────────────────────────────────────────
//  Supabase configuration injection
//
//  Reads the supabase.* keys from local.properties, falling back to
//  SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_GOOGLE_WEB_CLIENT_ID /
//  SUPABASE_DESKTOP_REDIRECT_PORT environment variables, and generates
//  SupabaseConfig.kt. Keeping these values out of VCS separates
//  per-developer/per-CI project configuration. See supabase/README.md.
// ─────────────────────────────────────────────────────────────────────────
fun parsePropertiesFile(text: String): Map<String, String> {
    val props = Properties()
    props.load(text.byteInputStream())
    return props.entries.associate { it.key.toString() to it.value.toString() }
}

fun supabaseConfigValue(properties: Map<String, String>, key: String, envName: String, fallback: String = ""): String =
    (properties[key] ?: System.getenv(envName) ?: fallback)
        .trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

fun registerSupabaseConfigTask(values: Map<String, String>, outputDirectory: File): TaskProvider<Task> =
    tasks.register("generateSupabaseConfig") {
        inputs.properties(values)
        outputs.dir(outputDirectory)
        doLast {
            val file = outputDirectory.resolve("com/dillu/quranlearner/cloud/SupabaseConfig.kt")
            file.parentFile.mkdirs()
            file.writeText(
                """
                |// GENERATED FILE — do not edit; values come from local.properties (see supabase/README.md).
                |package com.dillu.quranlearner.cloud
                |
                |internal object SupabaseConfig {
                |    const val URL: String = "${values["SUPABASE_URL"]}"
                |    const val ANON_KEY: String = "${values["SUPABASE_ANON_KEY"]}"
                |    const val GOOGLE_WEB_CLIENT_ID: String = "${values["SUPABASE_GOOGLE_WEB_CLIENT_ID"]}"
                |    const val DESKTOP_REDIRECT_PORT: Int = ${values["SUPABASE_DESKTOP_REDIRECT_PORT"]}
                |
                |    val isConfigured: Boolean get() = URL.isNotBlank() && ANON_KEY.isNotBlank()
                |}
                |""".trimMargin()
            )
        }
    }

val supabaseConfigDir = layout.buildDirectory.dir("generated/supabaseConfig")
val supabaseReadProperties: Map<String, String> =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText
        .orElse("")
        .map { parsePropertiesFile(it) }
        .get() // read at configuration time so changing local.properties invalidates the configuration cache
val supabaseGeneratedValues = mapOf(
    "SUPABASE_URL" to supabaseConfigValue(supabaseReadProperties, "supabase.url", "SUPABASE_URL"),
    "SUPABASE_ANON_KEY" to supabaseConfigValue(supabaseReadProperties, "supabase.anonKey", "SUPABASE_ANON_KEY"),
    "SUPABASE_GOOGLE_WEB_CLIENT_ID" to supabaseConfigValue(supabaseReadProperties, "supabase.googleWebClientId", "SUPABASE_GOOGLE_WEB_CLIENT_ID"),
    "SUPABASE_DESKTOP_REDIRECT_PORT" to
        supabaseConfigValue(supabaseReadProperties, "supabase.desktopRedirectPort", "SUPABASE_DESKTOP_REDIRECT_PORT", "48210")
            .filter { it.isDigit() }.ifBlank { "48210" },
)
val generateSupabaseConfig = registerSupabaseConfigTask(
    supabaseGeneratedValues,
    supabaseConfigDir.get().asFile.resolve("kotlin"),
)
kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(supabaseConfigDir.map { it.dir("kotlin") })
tasks.withType<KotlinCompilationTask<*>>().configureEach { dependsOn(generateSupabaseConfig) }

// The KMP desktop test configurations misresolve the multiplatform androidx.sqlite "root"
// module to its Android variant; redirect it to the -jvm artifact explicitly.
configurations.configureEach {
    if (name == "desktopTestRuntimeClasspath" || name == "desktopTestCompileClasspath") {
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.sqlite" && requested.name == "sqlite") {
                useTarget("androidx.sqlite:sqlite-jvm:${requested.version}")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.dillu.quranlearner.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "com.dillu.quranlearner"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.compose.uiTooling)
}
