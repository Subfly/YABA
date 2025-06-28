import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(17)

    androidTarget { publishLibraryVariants("release") }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.serialization)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.server.logging)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.server.netty)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        macosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }

    //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations["main"].compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.add("-Xexport-kdoc")
            }
        }
    }

}

android {
    namespace = "dev.subfly.yabasync"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

//Publishing your Kotlin Multiplatform library to Maven Central
//https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    coordinates("dev.subfly.yabasync", "YABASyncCore", "1.0.0")

    pom {
        name = "YABASync"
        description = "Kotlin Multiplatform library"
        url = "github url" //todo

        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }

        developers {
            developer {
                id = "" //todo
                name = "" //todo
                email = "" //todo
            }
        }

        scm {
            url = "github url" //todo
        }
    }
    if (project.hasProperty("signing.keyId")) signAllPublications()
}
