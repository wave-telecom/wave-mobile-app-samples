rootProject.name = "Sample"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val localFlowWrapperSdkRepo = file("../../wave-mobile-kmp/flow-wrapper-kmp/build/maven-repo")
val localNativeFlowsSdkRoot = file("../../wave-mobile-kmp-flows-compose")

includeBuild(localNativeFlowsSdkRoot) {
    dependencySubstitution {
        substitute(module("br.com.wave:flows-sdk")).using(project(":flows-sdk"))
    }
}

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        if (localFlowWrapperSdkRepo.exists()) {
            maven {
                url = uri(localFlowWrapperSdkRepo)
            }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://wave-telecom.github.io/wave-mobile-sdk-releases")
        }
    }
}

include(":composeApp")
