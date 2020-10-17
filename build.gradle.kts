plugins {
    kotlin("jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "0.12.0"
}

gradlePlugin {
    plugins {
        create("tReflectionGradlePlugin") {
            id = "ie.trinitcore.trinreflection.gradleplugin"
            implementationClass = "ie.trinitcore.trinreflection.gradleplugin.TReflectionGradlePlugin"
        }
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlinx")
    }
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers")
    }
}

dependencies {
    implementation(project(":trin-reflection:api"))
}

pluginBundle {
    website = "https://www.trinitcore.com/treflection"
    description = "Kotlin Reflection tools by Trinitcore"

    (plugins) {

        "tReflectionGradlePlugin" {
            displayName = "TReflection"
            tags = listOf("individual", "tags", "per", "plugin")
            version = "1.0"
        }

    }
}