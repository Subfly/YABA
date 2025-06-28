plugins {
    alias(libs.plugins.multiplatform)
}

kotlin {
    listOf(
        macosX64(),
        macosArm64(),
    ).forEach {
        it.binaries.executable {
            entryPoint = "main"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":YABASyncCore"))
        }
    }
}
