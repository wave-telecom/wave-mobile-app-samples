import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val flowWrapperVersion = "0.5.7"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("br.com.wave:flow-wrapper-kmp:$flowWrapperVersion")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val generateFlowWrapperVersion by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/source/flowWrapperVersion/commonMain/kotlin/br/com/wave/sample")
    inputs.property("flowWrapperVersion", flowWrapperVersion)
    outputs.dir(outputDir)

    doLast {
        val outputFile = outputDir.get().file("FlowWrapperVersion.kt").asFile
        val version = inputs.properties["flowWrapperVersion"] as String
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package br.com.wave.sample

            const val FLOW_WRAPPER_VERSION = "$version"
            """.trimIndent(),
        )
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(generateFlowWrapperVersion)

android {
    namespace = "br.com.wave.sample"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "br.com.wave.sample"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}
